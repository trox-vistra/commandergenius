/*
Simple DirectMedia Layer
Java source code (C) 2009-2014 Sergii Pylypenko

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required. 
2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/

package net.sourceforge.clonekeenplus;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.os.Vibrator;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.util.Log;
import android.widget.TextView;
import android.os.Build;
import java.util.Arrays;


class AccelerometerReader implements SensorEventListener
{

	private SensorManager _manager = null;
	public boolean openedBySDL = false;
	public static final GyroscopeListener gyro = new GyroscopeListener();
	public static final OrientationListener orientation = new OrientationListener();

	public AccelerometerReader(Activity context)
	{
		_manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	public synchronized void stop()
	{
		if( _manager != null )
		{
			Log.i("SDL", "libSDL: stopping accelerometer/gyroscope/orientation");
			_manager.unregisterListener(this);
			_manager.unregisterListener(gyro);
			_manager.unregisterListener(orientation);
		}
	}

	public synchronized void start()
	{
		if( (Globals.UseAccelerometerAsArrowKeys || Globals.AppUsesAccelerometer) &&
			_manager != null && _manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null )
		{
			Log.i("SDL", "libSDL: starting accelerometer");
			_manager.registerListener(this, _manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
		}
		if( (Globals.AppUsesGyroscope || Globals.MoveMouseWithGyroscope) &&
			_manager != null && _manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null )
		{
			Log.i("SDL", "libSDL: starting gyroscope");
			_manager.registerListener(gyro, _manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
		}
		if( (Globals.AppUsesOrientationSensor) && _manager != null &&
			_manager.getDefaultSensor(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? Sensor.TYPE_GAME_ROTATION_VECTOR : Sensor.TYPE_ROTATION_VECTOR) != null )
		{
			Log.i("SDL", "libSDL: starting orientation sensor");
			_manager.registerListener(orientation, _manager.getDefaultSensor(
				Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? Sensor.TYPE_GAME_ROTATION_VECTOR : Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_GAME);
		}
	}

	public void onSensorChanged(SensorEvent event)
	{
		if( Globals.HorizontalOrientation )
		{
			if( gyro.invertedOrientation )
				nativeAccelerometer(-event.values[1], event.values[0], event.values[2]);
			else
				nativeAccelerometer(event.values[1], -event.values[0], event.values[2]);
		}
		else
			nativeAccelerometer(event.values[0], event.values[1], event.values[2]); // TODO: not tested!
	}

	public void onAccuracyChanged(Sensor s, int a)
	{
	}

	static class GyroscopeListener implements SensorEventListener
	{
		public boolean invertedOrientation = false;

		final float noiseMin[] = new float[] { -1.0f, -1.0f, -1.0f }; // Large initial values, they will only decrease
		final float noiseMax[] = new float[] { 1.0f, 1.0f, 1.0f };

		float noiseData[][] = new float[200][3];
		int noiseDataIdx = noiseData.length * 3 / 4; // Speed up first measurement, to converge to sane values faster
		int noiseMovementDetected = 0;
		float noiseMeasuredRange[] = null;
		
		static int noiseCounter = 0;

		public GyroscopeListener()
		{
		}

		void collectNoiseData(final float[] data)
		{
			for( int i = 0; i < 3; i++ )
			{
				if( data[i] < noiseMin[i] || data[i] > noiseMax[i] )
				{
					// Movement detected, this can converge our min/max too early, so we're discarding last few values
					if( noiseMovementDetected < 0 )
					{
						int discard = 10;
						if( -noiseMovementDetected < discard )
							discard = -noiseMovementDetected;
						noiseDataIdx -= discard;
						if( noiseDataIdx < 0 )
							noiseDataIdx = 0;
					}
					noiseMovementDetected = 10;
					return;
				}
				noiseData[noiseDataIdx][i] = data[i];
			}
			noiseMovementDetected--;
			if( noiseMovementDetected >= 0 )
				return; // Also discard several values after the movement stopped
			noiseDataIdx++;

			if( noiseDataIdx < noiseData.length )
				return;

			noiseCounter++;
			Log.i( "SDL", "GYRO_NOISE: Measuring in progress... " + noiseCounter ); // DEBUG
			if( noiseCounter > 15 )
			{
				Log.i( "SDL", "GYRO_NOISE: Measuring done! Max iteration reached " + noiseCounter ); // DEBUG
				noiseData = null;
				noiseMeasuredRange = null;
			}

			noiseDataIdx = 0;
			boolean changed = false;
			for( int i = 0; i < 3; i++ )
			{
				float min = 1.0f;
				float max = -1.0f;
				for( int ii = 0; ii < noiseData.length; ii++ )
				{
					if( min > noiseData[ii][i] )
						min = noiseData[ii][i];
					if( max < noiseData[ii][i] )
						max = noiseData[ii][i];
				}
				// Increase the range a bit, for conservative noise filtering
				float middle = (min + max) / 2.0f;
				min += (min - middle) * 0.2f;
				max += (max - middle) * 0.2f;
				// Check if range between min/max is less then the current range, as a safety measure,
				// and min/max range is not jumping outside of previously measured range
				if( max - min < noiseMax[i] - noiseMin[i] && min >= noiseMin[i] && max <= noiseMax[i] )
				{
					noiseMax[i] = (noiseMax[i] + max * 4.0f) / 5.0f;
					noiseMin[i] = (noiseMin[i] + min * 4.0f) / 5.0f;
					changed = true;
				}
			}

			Log.i( "SDL", "GYRO_NOISE: MIN MAX: " + Arrays.toString(noiseMin) + " " + Arrays.toString(noiseMax) ); // DEBUG

			if( !changed )
				return;

			// Determine when to stop measuring - check that the previous min/max range is close to the current one

			float range[] = new float[3];
			for( int i = 0; i < 3; i++ )
				range[i] = noiseMax[i] - noiseMin[i];

			Log.i( "SDL", "GYRO_NOISE: RANGE:   " + Arrays.toString(range) + " " + Arrays.toString(noiseMeasuredRange) ); // DEBUG

			if( noiseMeasuredRange == null )
			{
				noiseMeasuredRange = range;
				return;
			}

			for( int i = 0; i < 3; i++ )
			{
				if( noiseMeasuredRange[i] / range[i] > 1.2f )
				{
					noiseMeasuredRange = range;
					return;
				}
			}

			// We converged to the final min/max, stop measuring
			noiseData = null;
			noiseMeasuredRange = null;
			Log.i( "SDL", "GYRO_NOISE: Measuring done! Range converged on iteration " + noiseCounter ); // DEBUG
		}

		public void onSensorChanged(final SensorEvent event)
		{
			boolean filtered = true;
			final float[] data = event.values;

			if( noiseData != null )
				collectNoiseData(data);

			for( int i = 0; i < 3; i++ )
			{
				if( data[i] < noiseMin[i] )
				{
					filtered = false;
					data[i] -= noiseMin[i];
				}
				else if( data[i] > noiseMax[i] )
				{
					filtered = false;
					data[i] -= noiseMax[i];
				}
			}

			if( filtered )
				return;

			if( Globals.HorizontalOrientation )
			{
				if( invertedOrientation )
					nativeGyroscope(-data[0], -data[1], data[2]);
				else
					nativeGyroscope(data[0], data[1], data[2]);
			}
			else
			{
				if( invertedOrientation )
					nativeGyroscope(-data[1], data[0], data[2]);
				else
					nativeGyroscope(data[1], -data[0], data[2]);
			}
		}

		public void onAccuracyChanged(Sensor s, int a)
		{
		}
		public boolean available(Activity context)
		{
			SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
			return ( manager != null && manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null );
		}
		public void registerListener(Activity context, SensorEventListener l)
		{
			SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
			if ( manager == null && manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null )
				return;
			manager.registerListener(gyro, manager.getDefaultSensor(
				Globals.AppUsesOrientationSensor ? Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ?
				Sensor.TYPE_GAME_ROTATION_VECTOR : Sensor.TYPE_ROTATION_VECTOR : Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_GAME);
		}
		public void unregisterListener(Activity context,SensorEventListener l)
		{
			SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
			if ( manager == null )
				return;
			manager.unregisterListener(l);
		}
	}

	static class OrientationListener implements SensorEventListener
	{
		public OrientationListener()
		{
		}
		public void onSensorChanged(SensorEvent event)
		{
			nativeOrientation(event.values[0], event.values[1], event.values[2]);
		}
		public void onAccuracyChanged(Sensor s, int a)
		{
		}
	}

	private static native void nativeAccelerometer(float accX, float accY, float accZ);
	private static native void nativeGyroscope(float X, float Y, float Z);
	private static native void nativeOrientation(float X, float Y, float Z);
}
