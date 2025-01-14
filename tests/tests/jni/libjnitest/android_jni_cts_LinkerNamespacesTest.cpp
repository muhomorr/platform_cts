/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Tests accessibility of platform native libraries
 */

#include <dirent.h>
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <libgen.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

#include <queue>
#include <regex>
#include <string>
#include <unordered_set>
#include <vector>

#include <android-base/file.h>
#include <android-base/properties.h>
#include <android-base/strings.h>
#include <nativehelper/scoped_local_ref.h>
#include <nativehelper/scoped_utf_chars.h>

#if defined(__LP64__)
#define LIB_DIR "lib64"
#else
#define LIB_DIR "lib"
#endif

static const std::string kSystemLibraryPath = "/system/" LIB_DIR;
static const std::string kVendorLibraryPath = "/vendor/" LIB_DIR;
static const std::string kProductLibraryPath = "/product/" LIB_DIR;

// APEX library paths to check for either presence or absence of public
// libraries.
static const std::vector<std::string> kApexLibraryPaths = {
  "/apex/com.android.art/" LIB_DIR,
  "/apex/com.android.i18n/" LIB_DIR,
  "/apex/com.android.neuralnetworks/" LIB_DIR,
  "/apex/com.android.runtime/" LIB_DIR,
};

static const std::vector<std::regex> kSystemPathRegexes = {
    std::regex("/system/lib(64)?"),
    std::regex("/apex/com\\.android\\.[^/]*/lib(64)?"),
    std::regex("/system/(lib/arm|lib64/arm64)"), // when CTS runs in ARM ABI on non-ARM CPU. http://b/149852946
};

// Full paths to libraries in system or APEX search paths that are not public
// but still may or may not be possible to load in an app.
static const std::vector<std::string> kOtherLoadableLibrariesInSearchPaths = {
  // This library may be loaded using DF_1_GLOBAL into the global group in
  // app_process, which is necessary to make it override some symbols in libc in
  // all DSO's. As a side effect it also gets inherited into the classloader
  // namespaces constructed in libnativeloader, and is hence possible to dlopen
  // even though there is no linker namespace link for it.
  "/apex/com.android.art/" LIB_DIR "/libsigchain.so",
};

static const std::string kWebViewPlatSupportLib = "libwebviewchromium_plat_support.so";

static bool not_accessible(const std::string& err) {
  return err.find("dlopen failed: library \"") == 0 &&
         err.find("is not accessible for the namespace \"") != std::string::npos;
}

static bool not_found(const std::string& err) {
  return err.find("dlopen failed: library \"") == 0 &&
         err.find("\" not found") != std::string::npos;
}

static bool wrong_arch(const std::string& library, const std::string& err) {
  // https://issuetracker.google.com/37428428
  // It's okay to not be able to load a library because it's for another
  // architecture (typically on an x86 device, when we come across an arm library).
  return err.find("dlopen failed: \"" + library + "\" has unexpected e_machine: ") == 0;
}

static bool is_library_on_path(const std::unordered_set<std::string>& library_search_paths,
                               const std::string& baselib,
                               const std::string& path) {
  std::string tail = '/' + baselib;
  if (!android::base::EndsWith(path, tail)) return false;
  return library_search_paths.count(path.substr(0, path.size() - tail.size())) > 0;
}

static std::string try_dlopen(const std::string& path) {
  // try to load the lib using dlopen().
  void *handle = dlopen(path.c_str(), RTLD_NOW);
  std::string error;

  bool loaded_in_native = handle != nullptr;
  if (loaded_in_native) {
    dlclose(handle);
  } else {
    error = dlerror();
  }
  return error;
}

// Tests if a file can be loaded or not. Returns empty string on success. On any failure
// returns the error message from dlerror().
static std::string load_library(JNIEnv* env, jclass clazz, const std::string& path,
                                bool test_system_load_library) {
  std::string error = try_dlopen(path);
  bool loaded_in_native = error.empty();

  if (android::base::EndsWith(path, '/' + kWebViewPlatSupportLib)) {
    // Don't try to load this library from Java. Otherwise, the lib is initialized via
    // JNI_OnLoad and it fails since WebView is not loaded in this test process.
    return error;
  }

  // try to load the same lib using System.load() in Java to see if it gives consistent
  // result with dlopen.
  static jmethodID java_load =
      env->GetStaticMethodID(clazz, "loadWithSystemLoad", "(Ljava/lang/String;)Ljava/lang/String;");
  ScopedLocalRef<jstring> jpath(env, env->NewStringUTF(path.c_str()));
  jstring java_load_errmsg = jstring(env->CallStaticObjectMethod(clazz, java_load, jpath.get()));
  bool java_load_ok = env->GetStringLength(java_load_errmsg) == 0;

  jstring java_load_lib_errmsg;
  bool java_load_lib_ok = java_load_ok;
  if (test_system_load_library && java_load_ok) {
    // If System.load() works then test System.loadLibrary() too. Cannot test
    // the other way around since System.loadLibrary() might very well find the
    // library somewhere else and hence work when System.load() fails.
    std::string baselib = basename(path.c_str());
    ScopedLocalRef<jstring> jname(env, env->NewStringUTF(baselib.c_str()));
    static jmethodID java_load_lib = env->GetStaticMethodID(
        clazz, "loadWithSystemLoadLibrary", "(Ljava/lang/String;)Ljava/lang/String;");
    java_load_lib_errmsg = jstring(env->CallStaticObjectMethod(clazz, java_load_lib, jname.get()));
    java_load_lib_ok = env->GetStringLength(java_load_lib_errmsg) == 0;
  }

  if (loaded_in_native != java_load_ok || java_load_ok != java_load_lib_ok) {
    const std::string java_load_error(ScopedUtfChars(env, java_load_errmsg).c_str());
    error = "Inconsistent result for library \"" + path + "\": dlopen() " +
            (loaded_in_native ? "succeeded" : "failed (" + error + ")") +
            ", System.load() " +
            (java_load_ok ? "succeeded" : "failed (" + java_load_error + ")");
    if (test_system_load_library) {
      const std::string java_load_lib_error(ScopedUtfChars(env, java_load_lib_errmsg).c_str());
      error += ", System.loadLibrary() " +
               (java_load_lib_ok ? "succeeded" : "failed (" + java_load_lib_error + ")");
    }
  }

  if (loaded_in_native && java_load_ok) {
    // Unload the shared lib loaded in Java. Since we don't have a method in Java for unloading a
    // lib other than destroying the classloader, here comes a trick; we open the same library
    // again with dlopen to get the handle for the lib and then calls dlclose twice (since we have
    // opened the lib twice; once in Java, once in here). This works because dlopen returns the
    // the same handle for the same shared lib object.
    void* handle = dlopen(path.c_str(), RTLD_NOW);
    dlclose(handle);
    dlclose(handle); // don't delete this line. it's not a mistake (see comment above).
  }

  return error;
}

static bool skip_subdir_load_check(const std::string& path) {
  static bool vndk_lite = android::base::GetBoolProperty("ro.vndk.lite", false);
  static const std::string system_vndk_dir = kSystemLibraryPath + "/vndk-sp-";
  return vndk_lite && android::base::StartsWith(path, system_vndk_dir);
}

// Checks that a .so library can or cannot be loaded with dlopen() and
// System.load(), as appropriate by the other settings:
// -  clazz: The java class instance of android.jni.cts.LinkerNamespacesHelper,
//    used for calling System.load() and System.loadLibrary().
// -  path: Full path to the library to load.
// -  library_search_paths: Directories that should be searched for public
//    libraries. They should not be loaded from a subdirectory of these.
// -  public_library_basenames: File names without paths of expected public
//    libraries.
// -  test_system_load_library: Try loading with System.loadLibrary() as well.
// -  check_absence: Raise an error if it is a non-public library but still is
//    loaded successfully from a searched directory.
static bool check_lib(JNIEnv* env,
                      jclass clazz,
                      const std::string& path,
                      const std::unordered_set<std::string>& library_search_paths,
                      const std::unordered_set<std::string>& public_library_basenames,
                      bool test_system_load_library,
                      bool check_absence,
                      /*out*/ std::vector<std::string>* errors) {
  std::string err = load_library(env, clazz, path, test_system_load_library);
  bool loaded = err.empty();

  // The current restrictions on public libraries:
  //  - It must exist only in the top level directory of "library_search_paths".
  //  - No library with the same name can be found in a sub directory.
  //  - Each public library does not contain any directory components.

  std::string baselib = basename(path.c_str());
  bool is_public = public_library_basenames.find(baselib) != public_library_basenames.end();

  // Special casing for symlinks in APEXes. For bundled APEXes, some files in
  // the APEXes could be symlinks pointing to libraries in /system/lib to save
  // storage. In that case, use the realpath so that `is_in_search_path` is
  // correctly determined
  bool is_in_search_path;
  std::string realpath;
  if (android::base::StartsWith(path, "/apex/") && android::base::Realpath(path, &realpath)) {
    is_in_search_path = is_library_on_path(library_search_paths, baselib, realpath);
  } else {
    is_in_search_path = is_library_on_path(library_search_paths, baselib, path);
  }

  if (is_public) {
    if (is_in_search_path) {
      if (!loaded) {
        errors->push_back("The library \"" + path +
                          "\" is a public library but it cannot be loaded: " + err);
        return false;
      }
    } else {  // !is_in_search_path
      if (loaded && !skip_subdir_load_check(path)) {
        errors->push_back("The library \"" + path +
                          "\" is a public library that was loaded from a subdirectory.");
        return false;
      }
    }
  } else {  // !is_public
    // If the library loaded successfully but is in a subdirectory then it is
    // still not public. That is the case e.g. for
    // /apex/com.android.runtime/lib{,64}/bionic/lib*.so.
    if (loaded && is_in_search_path && check_absence &&
        (std::find(kOtherLoadableLibrariesInSearchPaths.begin(),
                   kOtherLoadableLibrariesInSearchPaths.end(), path) ==
         kOtherLoadableLibrariesInSearchPaths.end())) {
      errors->push_back("The library \"" + path + "\" is not a public library but it loaded.");
      return false;
    }
  }

  if (!loaded && !not_accessible(err) && !not_found(err) && !wrong_arch(path, err)) {
    errors->push_back("unexpected dlerror: " + err);
    return false;
  }

  return true;
}

// Calls check_lib for every file found recursively within library_path.
static bool check_path(JNIEnv* env,
                       jclass clazz,
                       const std::string& library_path,
                       const std::unordered_set<std::string>& library_search_paths,
                       const std::unordered_set<std::string>& public_library_basenames,
                       bool test_system_load_library,
                       bool check_absence,
                       /*out*/ std::vector<std::string>* errors) {
  bool success = true;
  std::queue<std::string> dirs;
  dirs.push(library_path);
  while (!dirs.empty()) {
    std::string dir = dirs.front();
    dirs.pop();

    std::unique_ptr<DIR, decltype(&closedir)> dirp(opendir(dir.c_str()), closedir);
    if (dirp == nullptr) {
      errors->push_back("Failed to open " + dir + ": " + strerror(errno));
      success = false;
      continue;
    }

    dirent* dp;
    while ((dp = readdir(dirp.get())) != nullptr) {
      // skip "." and ".."
      if (strcmp(".", dp->d_name) == 0 || strcmp("..", dp->d_name) == 0) {
        continue;
      }

      std::string path = dir + "/" + dp->d_name;
      // We cannot just load hwasan libraries into a non-hwasan process, so
      // we are skipping those.
      if (path.find("hwasan") != std::string::npos) {
        continue;
      }
      struct stat sb;
      // Use lstat to not dereference a symlink. If it links out of library_path
      // it can be ignored because the Bionic linker derefences symlinks before
      // checking the path. If it links inside library_path we'll get to the
      // link target anyway.
      if (lstat(path.c_str(), &sb) != -1) {
        if (S_ISDIR(sb.st_mode)) {
          dirs.push(path);
        } else if (!S_ISLNK(sb.st_mode) &&
                   !check_lib(env, clazz, path, library_search_paths, public_library_basenames,
                              test_system_load_library, check_absence, errors)) {
          success = false;
        }
      }
    }
  }

  return success;
}

static bool jobject_array_to_set(JNIEnv* env,
                                 jobjectArray java_libraries_array,
                                 std::unordered_set<std::string>* libraries,
                                 std::string* error_msgs) {
  error_msgs->clear();
  size_t size = env->GetArrayLength(java_libraries_array);
  bool success = true;
  for (size_t i = 0; i<size; ++i) {
    ScopedLocalRef<jstring> java_soname(
        env, (jstring) env->GetObjectArrayElement(java_libraries_array, i));
    std::string soname(ScopedUtfChars(env, java_soname.get()).c_str());

    // Verify that the name doesn't contain any directory components.
    if (soname.rfind('/') != std::string::npos) {
      *error_msgs += "\n---Illegal value, no directories allowed: " + soname;
      continue;
    }

    // Check to see if the string ends in " 32" or " 64" to indicate the
    // library is only public for one bitness.
    size_t space_pos = soname.rfind(' ');
    if (space_pos != std::string::npos) {
      std::string type = soname.substr(space_pos + 1);
      if (type != "32" && type != "64") {
        *error_msgs += "\n---Illegal value at end of line (only 32 or 64 allowed): " + soname;
        success = false;
        continue;
      }
#if defined(__LP64__)
      if (type == "32") {
        // Skip this, it's a 32 bit only public library.
        continue;
      }
#else
      if (type == "64") {
        // Skip this, it's a 64 bit only public library.
        continue;
      }
#endif
      soname.resize(space_pos);
    }

    libraries->insert(soname);
  }

  return success;
}

// This is not public function but only known way to get search path of the default namespace.
extern "C" void android_get_LD_LIBRARY_PATH(char*, size_t) __attribute__((__weak__));

extern "C" JNIEXPORT jstring JNICALL
    Java_android_jni_cts_LinkerNamespacesHelper_runAccessibilityTestImpl(
        JNIEnv* env,
        jclass clazz,
        jobjectArray java_system_public_libraries,
        jobjectArray java_apex_public_libraries) {
  bool success = true;
  std::vector<std::string> errors;
  std::string error_msgs;
  std::unordered_set<std::string> system_public_libraries;
  if (!jobject_array_to_set(env, java_system_public_libraries, &system_public_libraries,
                            &error_msgs)) {
    success = false;
    errors.push_back("Errors in system public library list:" + error_msgs);
  }
  std::unordered_set<std::string> apex_public_libraries;
  if (!jobject_array_to_set(env, java_apex_public_libraries, &apex_public_libraries,
                            &error_msgs)) {
    success = false;
    errors.push_back("Errors in APEX public library list:" + error_msgs);
  }

  // Check the system libraries.

  // Check current search path and add the rest of search path configured for
  // the default namepsace.
  char default_search_paths[PATH_MAX];
  android_get_LD_LIBRARY_PATH(default_search_paths, sizeof(default_search_paths));

  std::vector<std::string> library_search_paths = android::base::Split(default_search_paths, ":");

  // Remove everything pointing outside of /system/lib* and
  // /apex/com.android.*/lib*.
  std::unordered_set<std::string> system_library_search_paths;

  for (const std::string& path : library_search_paths) {
    for (const std::regex& regex : kSystemPathRegexes) {
      if (std::regex_match(path, regex)) {
        system_library_search_paths.insert(path);
        break;
      }
    }
  }

  // These paths should be tested too - this is because apps may rely on some
  // libraries being available there.
  system_library_search_paths.insert(kSystemLibraryPath);
  system_library_search_paths.insert(kApexLibraryPaths.begin(), kApexLibraryPaths.end());

  if (!check_path(env, clazz, kSystemLibraryPath, system_library_search_paths,
                  system_public_libraries,
                  /*test_system_load_library=*/false, /*check_absence=*/true, &errors)) {
    success = false;
  }

  // Pre-Treble devices use ld.config.vndk_lite.txt, where the default namespace
  // isn't isolated. That means it can successfully load libraries in /apex, so
  // don't complain about that in that case.
  bool check_absence = !android::base::GetBoolProperty("ro.vndk.lite", false);

  // Check the APEX libraries.
  for (const std::string& apex_path : kApexLibraryPaths) {
    if (!check_path(env, clazz, apex_path, {apex_path},
                    apex_public_libraries,
                    /*test_system_load_library=*/true,
                    check_absence, &errors)) {
      success = false;
    }
  }

  if (!success) {
    std::string error_str;
    for (const std::string& line : errors) {
      error_str += line + '\n';
    }
    return env->NewStringUTF(error_str.c_str());
  }

  return nullptr;
}

extern "C" JNIEXPORT jstring JNICALL Java_android_jni_cts_LinkerNamespacesHelper_tryDlopen(
        JNIEnv* env,
        jclass clazz,
        jstring lib) {
    ScopedUtfChars soname(env, lib);
    std::string error_str = try_dlopen(soname.c_str());

    if (!error_str.empty()) {
      return env->NewStringUTF(error_str.c_str());
    }
    return nullptr;
}

extern "C" JNIEXPORT jint JNICALL Java_android_jni_cts_LinkerNamespacesHelper_getLibAbi(
        JNIEnv* env,
        jclass clazz) {
#ifdef __aarch64__
    return 1; // ARM64
#elif __arm__
    return 2;
#elif __x86_64__
    return 3;
#elif i386
    return 4;
#else
    return 0;
#endif
}
