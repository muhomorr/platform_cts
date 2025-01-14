/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.graphics.cts;

import static android.graphics.PathIterator.VERB_CLOSE;
import static android.graphics.PathIterator.VERB_CONIC;
import static android.graphics.PathIterator.VERB_CUBIC;
import static android.graphics.PathIterator.VERB_DONE;
import static android.graphics.PathIterator.VERB_LINE;
import static android.graphics.PathIterator.VERB_MOVE;
import static android.graphics.PathIterator.VERB_QUAD;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.graphics.Path;
import android.graphics.PathIterator;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.compatibility.common.util.ApiTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ConcurrentModificationException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class PathIteratorTest {

    private Path mPath;
    private float[] mPoints = new float[8];

    @Before
    public void setup() {
        mPath = new Path();
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator"})
    public void testIteratorExists() {
        PathIterator iterator = mPath.getPathIterator();
        assertNotNull(iterator);
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator#hasNext"})
    public void testEmptyPath() {
        PathIterator iterator = mPath.getPathIterator();
        assertFalse(iterator.hasNext());
        assertEquals("Empty path should have no verbs",
                PathIterator.VERB_DONE, iterator.next(mPoints, 0));

        // Now with next()
        iterator = mPath.getPathIterator();
        assertFalse(iterator.hasNext());
        assertEquals("Empty path should have no verbs",
                PathIterator.VERB_DONE, iterator.next().getVerb());
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator.Segment#getVerb"
    })
    public void testMove() {
        mPath.moveTo(100f, 200f);
        PathIterator iterator;

        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_DONE, iterator.next(mPoints, 0));

        // Now with next()
        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next().getVerb());
        assertEquals(PathIterator.VERB_DONE, iterator.next().getVerb());
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator.Segment#getVerb"
    })
    public void testDoneDone() {
        mPath.lineTo(100f, 200f);
        int verbIndex = 0;
        for (PathIterator it = mPath.getPathIterator(); it.hasNext(); ) {
            PathIterator.Segment segment = it.next();
            int verb = segment.getVerb();
            switch (verbIndex) {
                case 0:
                    assertEquals(PathIterator.VERB_MOVE, verb);
                    break;
                case 1:
                    assertEquals(PathIterator.VERB_LINE, verb);
                    break;
            }
            verbIndex++;
        }
        assertTrue(verbIndex < 3);

        // Now with next(float[], int)
        PathIterator iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_LINE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_DONE, iterator.next(mPoints, 0));
        // An iterator with no more elements simply returns DONE on all subsequent next calls
        assertEquals(PathIterator.VERB_DONE, iterator.next(mPoints, 0));
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator.Segment#getVerb"
    })
    public void testPeek() {
        mPath.lineTo(100f, 200f);
        mPath.quadTo(300f, 400f, 500f, 600f);
        PathIterator iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.peek());
        // Calling peek() again should not change the next operation
        assertEquals(PathIterator.VERB_MOVE, iterator.peek());
        assertEquals(PathIterator.VERB_MOVE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_LINE, iterator.peek());
        assertEquals(PathIterator.VERB_LINE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_QUAD, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_DONE, iterator.peek());

        // Now with next()
        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.peek());
        // Calling peek() again should not change the next operation
        assertEquals(PathIterator.VERB_MOVE, iterator.peek());
        assertEquals(PathIterator.VERB_MOVE, iterator.next().getVerb());
        assertEquals(PathIterator.VERB_LINE, iterator.peek());
        assertEquals(PathIterator.VERB_LINE, iterator.next().getVerb());
        assertEquals(PathIterator.VERB_QUAD, iterator.next().getVerb());
        assertEquals(PathIterator.VERB_DONE, iterator.peek());
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator.Segment#getVerb",
            "android.graphics.PathIterator.Segment#getPoints"
    })
    public void testLine() {
        mPath.lineTo(100f, 200f);
        PathIterator iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_LINE, iterator.next(mPoints, 0));
        assertEquals(0f, mPoints[0], 0f);
        assertEquals(0f, mPoints[1], 0f);
        assertEquals(100f, mPoints[2], 0f);
        assertEquals(200f, mPoints[3], 0f);
        assertEquals(PathIterator.VERB_DONE, iterator.next(mPoints, 0));

        // Now with next()
        iterator = mPath.getPathIterator();
        PathIterator.Segment segment;
        segment = iterator.next();
        assertEquals(PathIterator.VERB_MOVE, segment.getVerb());
        segment = iterator.next();
        assertEquals(PathIterator.VERB_LINE, segment.getVerb());
        float[] points = segment.getPoints();
        assertEquals(0f, points[0], 0f);
        assertEquals(0f, points[1], 0f);
        assertEquals(100f, points[2], 0f);
        assertEquals(200f, points[3], 0f);
        segment = iterator.next();
        assertEquals(PathIterator.VERB_DONE, segment.getVerb());
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator.Segment#getVerb",
            "android.graphics.PathIterator.Segment#getPoints",
            "android.graphics.PathIterator.Segment#getConicWeight",
    })
    public void testIterable() {
        mPath.lineTo(100f, 200f);
        mPath.conicTo(300f, 400f, 500f, 600f, 2f);
        int verbIndex = 0;
        for (PathIterator it = mPath.getPathIterator(); it.hasNext(); ) {
            PathIterator.Segment segment = it.next();
            int verb = segment.getVerb();
            float[] points = segment.getPoints();
            float weight = segment.getConicWeight();
            switch (verbIndex) {
                case 0:
                    assertEquals(PathIterator.VERB_MOVE, verb);
                    break;
                case 1:
                    assertEquals(PathIterator.VERB_LINE, verb);
                    assertEquals(0f, points[0], 0f);
                    assertEquals(0f, points[1], 0f);
                    assertEquals(100f, points[2], 0f);
                    assertEquals(200f, points[3], 0f);
                    break;
                case 2:
                    assertEquals(PathIterator.VERB_CONIC, verb);
                    assertEquals(100f, points[0], 0f);
                    assertEquals(200f, points[1], 0f);
                    assertEquals(300f, points[2], 0f);
                    assertEquals(400f, points[3], 0f);
                    assertEquals(500f, points[4], 0f);
                    assertEquals(600f, points[5], 0f);
                    assertEquals(2f, weight, 0f);
                    break;
                case 3:
                    assertEquals(PathIterator.VERB_DONE, verb);
                    break;
            }
            verbIndex++;
            if (verbIndex > 3) break;
        }
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator.Segment#getVerb",
            "android.graphics.PathIterator.Segment#getPoints",
    })
    public void testQuad() {
        mPath.quadTo(100f, 200f, 300f, 400f);
        PathIterator iterator;
        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_QUAD, iterator.next(mPoints, 0));
        assertEquals(0f, mPoints[0], 0f);
        assertEquals(0f, mPoints[1], 0f);
        assertEquals(100f, mPoints[2], 0f);
        assertEquals(200f, mPoints[3], 0f);
        assertEquals(300f, mPoints[4], 0f);
        assertEquals(400f, mPoints[5], 0f);
        assertEquals(PathIterator.VERB_DONE, iterator.next(mPoints, 0));

        // Now with next()
        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next().getVerb());
        PathIterator.Segment segment = iterator.next();
        assertEquals(PathIterator.VERB_QUAD, segment.getVerb());
        float[] points = segment.getPoints();
        assertEquals(0f, points[0], 0f);
        assertEquals(0f, points[1], 0f);
        assertEquals(100f, points[2], 0f);
        assertEquals(200f, points[3], 0f);
        assertEquals(300f, points[4], 0f);
        assertEquals(400f, points[5], 0f);
        assertEquals(PathIterator.VERB_DONE, iterator.next().getVerb());
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator.Segment#getVerb",
            "android.graphics.PathIterator.Segment#getPoints",
            "android.graphics.PathIterator.Segment#getConicWeight",
    })
    public void testConic() {
        mPath.conicTo(100f, 200f, 300f, 400f, 2f);
        PathIterator iterator;
        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_CONIC, iterator.next(mPoints, 0));
        assertEquals(0f, mPoints[0], 0f);
        assertEquals(0f, mPoints[1], 0f);
        assertEquals(100f, mPoints[2], 0f);
        assertEquals(200f, mPoints[3], 0f);
        assertEquals(300f, mPoints[4], 0f);
        assertEquals(400f, mPoints[5], 0f);
        assertEquals(2f, mPoints[6], 0f);
        assertEquals(PathIterator.VERB_DONE, iterator.next(mPoints, 0));

        // Now with next()
        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next().getVerb());
        PathIterator.Segment segment = iterator.next();
        assertEquals(PathIterator.VERB_CONIC, segment.getVerb());
        float[] points = segment.getPoints();
        assertEquals(0f, points[0], 0f);
        assertEquals(0f, points[1], 0f);
        assertEquals(100f, points[2], 0f);
        assertEquals(200f, points[3], 0f);
        assertEquals(300f, points[4], 0f);
        assertEquals(400f, points[5], 0f);
        assertEquals(2f, segment.getConicWeight(), 0f);
        assertEquals(PathIterator.VERB_DONE, iterator.next().getVerb());
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator.Segment#getVerb",
            "android.graphics.PathIterator.Segment#getPoints",
    })
    public void testCubic() {
        mPath.cubicTo(100f, 200f, 300f, 400f, 500f, 600f);
        PathIterator iterator;
        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_CUBIC, iterator.next(mPoints, 0));
        assertEquals(0f, mPoints[0], 0f);
        assertEquals(0f, mPoints[1], 0f);
        assertEquals(100f, mPoints[2], 0f);
        assertEquals(200f, mPoints[3], 0f);
        assertEquals(300f, mPoints[4], 0f);
        assertEquals(400f, mPoints[5], 0f);
        assertEquals(500f, mPoints[6], 0f);
        assertEquals(600f, mPoints[7], 0f);
        assertEquals(PathIterator.VERB_DONE, iterator.next(mPoints, 0));

        // Now with next()
        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next().getVerb());
        PathIterator.Segment segment = iterator.next();
        assertEquals(PathIterator.VERB_CUBIC, segment.getVerb());
        float[] points = segment.getPoints();
        assertEquals(0f, points[0], 0f);
        assertEquals(0f, points[1], 0f);
        assertEquals(100f, points[2], 0f);
        assertEquals(200f, points[3], 0f);
        assertEquals(300f, points[4], 0f);
        assertEquals(400f, points[5], 0f);
        assertEquals(500f, points[6], 0f);
        assertEquals(600f, points[7], 0f);
        assertEquals(PathIterator.VERB_DONE, iterator.next().getVerb());
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator.Segment#getVerb",
    })
    public void testClose() {
        mPath.quadTo(100f, 200f, 300f, 400f);
        mPath.close();
        PathIterator iterator;
        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_QUAD, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_CLOSE, iterator.next(mPoints, 0));
        assertEquals(PathIterator.VERB_DONE, iterator.next(mPoints, 0));

        // Now with next()
        iterator = mPath.getPathIterator();
        assertEquals(PathIterator.VERB_MOVE, iterator.next().getVerb());
        assertEquals(PathIterator.VERB_QUAD, iterator.next().getVerb());
        assertEquals(PathIterator.VERB_CLOSE, iterator.next().getVerb());
        assertEquals(PathIterator.VERB_DONE, iterator.next().getVerb());
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next"})
    public void testPathModification() {
        mPath.lineTo(100f, 200f);
        final PathIterator iterator = mPath.getPathIterator();
        mPath.lineTo(300f, 400);
        assertThrows(ConcurrentModificationException.class, () -> iterator.next(mPoints, 0));

        final PathIterator iterator1 = mPath.getPathIterator();
        mPath.reset();
        assertThrows(ConcurrentModificationException.class, () -> iterator1.next(mPoints, 0));

        // Now with next()
        final PathIterator iterator2 = mPath.getPathIterator();
        mPath.lineTo(300f, 400);
        assertThrows(ConcurrentModificationException.class, iterator2::next);

        final PathIterator iterator3 = mPath.getPathIterator();
        mPath.reset();
        assertThrows(ConcurrentModificationException.class, iterator3::next);
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next"})
    public void testPointsArray() {
        mPath.lineTo(100f, 200f);
        PathIterator iterator = mPath.getPathIterator();
        for (int i = 0; i < 8; ++i) {
            float[] smallArray = new float[i];
            assertThrows("Points array of size " + i + " too small",
                    ArrayIndexOutOfBoundsException.class, () -> iterator.next(smallArray, 0));
        }
        // Also check a proper-sized array with an offset that makes it too small
        for (int i = 1; i < 8; ++i) {
            final int offset = i;
            assertThrows("Points array with offset " + offset + " too small",
                    ArrayIndexOutOfBoundsException.class, () -> iterator.next(mPoints, offset));
        }
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next"})
    public void testPointsArrayOffset() {
        float[] pointsArray = new float[16];
        mPath.lineTo(100f, 200f);
        for (int offset = 0; offset < 8; ++offset) {
            PathIterator iterator = mPath.getPathIterator();
            assertEquals(PathIterator.VERB_MOVE, iterator.next(pointsArray, offset));
            assertEquals(PathIterator.VERB_LINE, iterator.next(pointsArray, offset));
            assertEquals(0f, pointsArray[offset + 0], 0f);
            assertEquals(0f, pointsArray[offset + 1], 0f);
            assertEquals(100f, pointsArray[offset + 2], 0f);
            assertEquals(200f, pointsArray[offset + 3], 0f);
            assertEquals(PathIterator.VERB_DONE, iterator.next(pointsArray, offset));
        }
    }

    @Test
    @ApiTest(apis = {"android.graphics.Path#getPathIterator", "android.graphics.PathIterator#next",
            "android.graphics.PathIterator.Segment#getVerb",
            "android.graphics.PathIterator.Segment#getPoints",
            "android.graphics.PathIterator.Segment#getConicWeight",
    })
    public void testRecreation() {
        mPath.moveTo(10f, 10f);
        mPath.quadTo(100f, 200f, 300f, 400f);
        mPath.lineTo(500f, 600f);
        mPath.cubicTo(700f, 800f, 900f, 1000f, 1100f, 1200f);
        mPath.conicTo(1f, 2f, 3f, 4f, 2f);
        mPath.close();
        PathIterator iterator = mPath.getPathIterator();
        Path pathCopy = new Path();
        boolean lineDone = false;
        var verb = iterator.next(mPoints, 0);
        while (verb != VERB_DONE) {
            lineDone = issuePathCommand(pathCopy, mPoints, lineDone, verb, mPoints[6]);
            verb = iterator.next(mPoints, 0);
        }
        assertPathsEqual(mPath, pathCopy);

        // Now with next()
        iterator = mPath.getPathIterator();
        pathCopy = new Path();
        lineDone = false;
        PathIterator.Segment segment = iterator.next();
        while (segment.getVerb() != PathIterator.VERB_DONE) {
            lineDone = issuePathCommand(pathCopy, segment.getPoints(), lineDone, segment.getVerb(),
                    segment.getConicWeight());
            segment = iterator.next();
        }
        assertPathsEqual(mPath, pathCopy);
    }

    private boolean issuePathCommand(Path pathCopy, float[] points, boolean lineDone,
            int verb, float conicWeight) {
        switch (verb) {
            case VERB_MOVE:
                pathCopy.moveTo(points[0], points[1]);
                break;
            case VERB_LINE:
                if (!lineDone) {
                    // Skia inserts a lineTo() when close() is issued. We should
                    // ignore that second lineTo() because it will make the paths unequal
                    // in operations if not in geometry
                    pathCopy.lineTo(points[2], points[3]);
                    lineDone = true;
                }
                break;
            case VERB_QUAD:
                pathCopy.quadTo(points[2], points[3], points[4], points[5]);
                break;
            case VERB_CONIC:
                pathCopy.conicTo(points[2], points[3], points[4], points[5], conicWeight);
                break;
            case VERB_CUBIC:
                pathCopy.cubicTo(
                        points[2], points[3], points[4], points[5], points[6], points[7]);
                break;
            case VERB_CLOSE:
                pathCopy.close();
                break;
        }
        return lineDone;
    }

    private void assertPathsEqual(Path path1, Path path2) {
        PathIterator iterator1 = path1.getPathIterator();
        PathIterator iterator2 = path2.getPathIterator();
        float[] points1 = new float[8];
        float[] points2 = new float[8];
        int verb1 = iterator1.next(points1, 0);
        int verb2 = iterator2.next(points2, 0);
        while (verb1 != PathIterator.VERB_DONE && verb2 != PathIterator.VERB_DONE) {
            assertEquals(verb1, verb2);
            switch (verb1) {
                case VERB_MOVE:
                    assertPointsEqual(points1, points2, 2);
                    break;
                case VERB_LINE:
                    assertPointsEqual(points1, points2, 4);
                    break;
                case VERB_CONIC:
                    assertPointsEqual(points1, points2, 8);
                    break;
                case VERB_QUAD:
                    assertPointsEqual(points1, points2, 6);
                    break;
                case VERB_CUBIC:
                    assertPointsEqual(points1, points2, 8);
                    break;
                case VERB_CLOSE:
                    assertPointsEqual(points1, points2, 0);
                    break;
            }
            verb1 = iterator1.next(points1, 0);
            verb2 = iterator2.next(points2, 0);
        }

        // Now with next()
        iterator1 = path1.getPathIterator();
        iterator2 = path2.getPathIterator();
        PathIterator.Segment segment1 = iterator1.next();
        PathIterator.Segment segment2 = iterator2.next();
        while ((verb1 = segment1.getVerb()) != PathIterator.VERB_DONE
                && (verb2 = segment2.getVerb()) != PathIterator.VERB_DONE) {
            points1 = segment1.getPoints();
            points2 = segment2.getPoints();
            assertEquals(verb1, verb2);
            switch (verb1) {
                case VERB_MOVE:
                    assertPointsEqual(points1, points2, 2);
                    break;
                case VERB_LINE:
                    assertPointsEqual(points1, points2, 4);
                    break;
                case VERB_CONIC:
                    assertPointsEqual(points1, points2, 8);
                    break;
                case VERB_QUAD:
                    assertPointsEqual(points1, points2, 6);
                    break;
                case VERB_CUBIC:
                    assertPointsEqual(points1, points2, 8);
                    break;
                case VERB_CLOSE:
                    assertPointsEqual(points1, points2, 0);
                    break;
            }
            segment1 = iterator1.next();
            segment2 = iterator2.next();
        }
    }

    private void assertPointsEqual(float[] points1, float[] points2, int numToCheck) {
        for (int i = 0; i < numToCheck; ++i) {
            assertEquals("point " + i + "not equal", points1[i], points2[i], 0f);
        }
    }
}
