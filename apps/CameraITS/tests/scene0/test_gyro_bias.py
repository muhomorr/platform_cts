# Copyright 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Verify if the gyro has stable output when device is stationary."""

import logging
import os
import time

import matplotlib
from matplotlib import pylab
from mobly import test_runner
import numpy

import its_base_test
import camera_properties_utils
import its_session_utils

_NAME = os.path.basename(__file__).split('.')[0]
_N = 20  # Number of samples averaged together, in the plot.
_NSEC_TO_SEC = 1E-9
_MEAN_THRESH = 0.01  # PASS/FAIL threshold for gyro mean drift
_VAR_THRESH = 0.001  # PASS/FAIL threshold for gyro variance drift


class GyroBiasTest(its_base_test.ItsBaseTest):
  """Test if the gyro has stable output when device is stationary.
  """

  def test_gyro_bias(self):
    with its_session_utils.ItsSession(
        device_id=self.dut.serial,
        camera_id=self.camera_id,
        hidden_physical_id=self.hidden_physical_id) as cam:
      props = cam.get_camera_properties()
      props = cam.override_with_hidden_physical_camera_props(props)
      # Only run test if the appropriate caps are claimed.
      camera_properties_utils.skip_unless(
          camera_properties_utils.sensor_fusion(props) and
          cam.get_sensors().get('gyro'))

      logging.debug('Collecting gyro events')
      cam.start_sensor_events()
      time.sleep(5)
      gyro_events = cam.get_sensor_events()['gyro']

    name_with_log_path = os.path.join(self.log_path, _NAME)
    nevents = (len(gyro_events) // _N) * _N
    gyro_events = gyro_events[:nevents]
    times = numpy.array([(e['time'] - gyro_events[0]['time'])*_NSEC_TO_SEC
                         for e in gyro_events])
    xs = numpy.array([e['x'] for e in gyro_events])
    ys = numpy.array([e['y'] for e in gyro_events])
    zs = numpy.array([e['z'] for e in gyro_events])

    # Group samples into size-N groups and average each together, to get rid
    # of individual random spikes in the data.
    times = times[_N // 2::_N]
    xs = xs.reshape(nevents // _N, _N).mean(1)
    ys = ys.reshape(nevents // _N, _N).mean(1)
    zs = zs.reshape(nevents // _N, _N).mean(1)

    # add y limits so plot doesn't look like amplified noise
    y_min = min([numpy.amin(xs), numpy.amin(ys), numpy.amin(zs), -_MEAN_THRESH])
    y_max = max([numpy.amax(xs), numpy.amax(ys), numpy.amax(xs), _MEAN_THRESH])

    pylab.figure()
    pylab.plot(times, xs, 'r', label='x')
    pylab.plot(times, ys, 'g', label='y')
    pylab.plot(times, zs, 'b', label='z')
    pylab.title(_NAME)
    pylab.xlabel('Time (seconds)')
    pylab.ylabel(f'Gyro readings (mean of {_N} samples)')
    pylab.ylim([y_min, y_max])
    pylab.ticklabel_format(axis='y', style='sci', scilimits=(-3, -3))
    pylab.legend()
    logging.debug('Saving plot')
    matplotlib.pyplot.savefig(f'{name_with_log_path}_plot.png')

    for samples in [xs, ys, zs]:
      mean = samples.mean()
      var = numpy.var(samples)
      logging.debug('mean: %.3e', mean)
      logging.debug('var: %.3e', var)
      if mean >= _MEAN_THRESH:
        raise AssertionError(f'mean: {mean}.3e, TOL={_MEAN_THRESH}')
      if var >= _VAR_THRESH:
        raise AssertionError(f'var: {var}.3e, TOL={_VAR_THRESH}')


if __name__ == '__main__':
  test_runner.main()
