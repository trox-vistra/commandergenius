// This string is autogenerated by ChangeAppSettings.sh, do not change spaces amount
package com.googlecode.opentyrian;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.util.Log;
import java.io.*;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Environment;
import android.os.StatFs;
import java.util.Locale;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

class Settings
{
	static String SettingsFileName = "libsdl-settings.cfg";

	static AlertDialog changeConfigAlert = null;
	static Thread changeConfigAlertThread = null;
	static boolean settingsLoaded = false;

	static void Save(final MainActivity p)
	{
		try {
			ObjectOutputStream out = new ObjectOutputStream(p.openFileOutput( SettingsFileName, p.MODE_WORLD_READABLE ));
			out.writeBoolean(Globals.DownloadToSdcard);
			out.writeBoolean(Globals.PhoneHasArrowKeys);
			out.writeBoolean(Globals.PhoneHasTrackball);
			out.writeBoolean(Globals.UseAccelerometerAsArrowKeys);
			out.writeBoolean(Globals.UseTouchscreenKeyboard);
			out.writeInt(Globals.TouchscreenKeyboardSize);
			out.writeInt(Globals.AccelerometerSensitivity);
			out.writeInt(Globals.TrackballDampening);
			out.writeInt(Globals.AudioBufferConfig);
			out.writeInt(Globals.OptionalDataDownload.length);
			for(int i = 0; i < Globals.OptionalDataDownload.length; i++)
				out.writeBoolean(Globals.OptionalDataDownload[i]);
			out.writeInt(Globals.TouchscreenKeyboardTheme);
			out.close();
			settingsLoaded = true;
			
		} catch( FileNotFoundException e ) {
		} catch( SecurityException e ) {
		} catch ( IOException e ) {};
	}

	static void Load( final MainActivity p )
	{
		if(settingsLoaded) // Prevent starting twice
		{
			startDownloader(p);
			return;
		}
		try {
			ObjectInputStream settingsFile = new ObjectInputStream(new FileInputStream( p.getFilesDir().getAbsolutePath() + "/" + SettingsFileName ));
			Globals.DownloadToSdcard = settingsFile.readBoolean();
			Globals.PhoneHasArrowKeys = settingsFile.readBoolean();
			Globals.PhoneHasTrackball = settingsFile.readBoolean();
			Globals.UseAccelerometerAsArrowKeys = settingsFile.readBoolean();
			Globals.UseTouchscreenKeyboard = settingsFile.readBoolean();
			Globals.TouchscreenKeyboardSize = settingsFile.readInt();
			Globals.AccelerometerSensitivity = settingsFile.readInt();
			Globals.TrackballDampening = settingsFile.readInt();
			Globals.AudioBufferConfig = settingsFile.readInt();
			Globals.OptionalDataDownload = new boolean[settingsFile.readInt()];
			for(int i = 0; i < Globals.OptionalDataDownload.length; i++)
				Globals.OptionalDataDownload[i] = settingsFile.readBoolean();
			Globals.TouchscreenKeyboardTheme = settingsFile.readInt();
			
			settingsLoaded = true;
			
			AlertDialog.Builder builder = new AlertDialog.Builder(p);
			builder.setTitle("Phone configuration");
			builder.setPositiveButton("Change phone configuration", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int item) 
				{
						changeConfigAlert = null;
						dialog.dismiss();
						showDownloadConfig(p);
				}
			});
			/*
			builder.setNegativeButton("Start", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int item) 
				{
						changeConfigAlert = null;
						dialog.dismiss();
						startDownloader(p);
				}
			});
			*/
			AlertDialog alert = builder.create();
			alert.setOwnerActivity(p);
			changeConfigAlert = alert;

			class Callback implements Runnable
			{
				MainActivity p;
				Callback( MainActivity _p ) { p = _p; }
				public void run()
				{
					try {
						Thread.sleep(2000);
					} catch( InterruptedException e ) {};
					if( changeConfigAlert == null )
						return;
					changeConfigAlert.dismiss();
					startDownloader(p);
				}
			};
			changeConfigAlertThread = new Thread(new Callback(p));
			changeConfigAlertThread.start();

			alert.show();

			return;
			
		} catch( FileNotFoundException e ) {
		} catch( SecurityException e ) {
		} catch ( IOException e ) {};
		
		// This code fails for both of my phones!
		/*
		Configuration c = new Configuration();
		c.setToDefaults();
		
		if( c.navigation == Configuration.NAVIGATION_TRACKBALL || 
			c.navigation == Configuration.NAVIGATION_DPAD ||
			c.navigation == Configuration.NAVIGATION_WHEEL )
		{
			Globals.AppNeedsArrowKeys = false;
		}
		
		System.out.println( "libSDL: Phone keypad type: " + 
				(
				c.navigation == Configuration.NAVIGATION_TRACKBALL ? "Trackball" :
				c.navigation == Configuration.NAVIGATION_DPAD ? "Dpad" :
				c.navigation == Configuration.NAVIGATION_WHEEL ? "Wheel" :
				c.navigation == Configuration.NAVIGATION_NONAV ? "None" :
				"Unknown" ) );
		*/

		showDownloadConfig(p);
	}

	static void showDownloadConfig(final MainActivity p) {

		long freeSdcard = 0;
		long freePhone = 0;
		try {
			StatFs sdcard = new StatFs(Environment.getExternalStorageDirectory().getPath());
			StatFs phone = new StatFs(Environment.getDataDirectory().getPath());
			freeSdcard = (long)sdcard.getAvailableBlocks() * sdcard.getBlockSize() / 1024 / 1024;
			freePhone = (long)phone.getAvailableBlocks() * phone.getBlockSize() / 1024 / 1024;
		}catch(Exception e) {}

		final CharSequence[] items = {"Phone storage - " + String.valueOf(freePhone) + " Mb free", "SD card - " + String.valueOf(freeSdcard) + " Mb free"};
		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		String [] downloadFiles = Globals.DataDownloadUrl.split("\\^");
		builder.setTitle(downloadFiles[0].split("[|]")[0]);
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.DownloadToSdcard = (item == 1);

				dialog.dismiss();
				showOptionalDownloadConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	};

	static void showOptionalDownloadConfig(final MainActivity p) {

		String [] downloadFiles = Globals.DataDownloadUrl.split("\\^");
		System.out.println("downloadFiles.length " + String.valueOf(downloadFiles.length));
		for(int i = 0; i < downloadFiles.length; i++)
			System.out.println("downloadFiles[" + String.valueOf(i) + "] = '" + downloadFiles[i] + "'");
		if(downloadFiles.length <= 1)
		{
			Globals.OptionalDataDownload = new boolean[1];
			Globals.OptionalDataDownload[0] = true;
			showKeyboardConfig(p);
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle("Optional packages");

		CharSequence[] items = new CharSequence[ downloadFiles.length - 1 ];
		for(int i = 1; i < downloadFiles.length; i++ )
			items[i-1] = new String(downloadFiles[i].split("[|]")[0]);

		if( Globals.OptionalDataDownload == null || Globals.OptionalDataDownload.length != items.length + 1 )
			Globals.OptionalDataDownload = new boolean[downloadFiles.length];
		Globals.OptionalDataDownload[0] = true;

		builder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() 
		{
			public void onClick(DialogInterface dialog, int item, boolean isChecked) 
			{
				System.out.println("Globals.OptionalDataDownload: set item " + String.valueOf(item + 1) + " to " + String.valueOf(isChecked));
				Globals.OptionalDataDownload[item+1] = isChecked;
			}
		});
		builder.setPositiveButton("Done", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{

				System.out.println("Globals.OptionalDataDownload len" + String.valueOf(Globals.OptionalDataDownload.length));
				for(int i = 0; i < Globals.OptionalDataDownload.length; i++)
					System.out.println("Globals.OptionalDataDownload[" + String.valueOf(i) + "] = '" + String.valueOf(Globals.OptionalDataDownload[i]) + "'");

				dialog.dismiss();
				showKeyboardConfig(p);
			}
		});

		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	};

	static void showKeyboardConfig(final MainActivity p)
	{
		if( ! Globals.AppNeedsArrowKeys )
		{
			showTrackballConfig(p);
			return;
		}
		
		final CharSequence[] items = {"Arrows / joystick / dpad", "Trackball", "None, only touchscreen"};

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle("What kind of navigation keys does your phone have?");
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.PhoneHasArrowKeys = (item == 0);
				Globals.PhoneHasTrackball = (item == 1);

				dialog.dismiss();
				showTrackballConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}

	static void showTrackballConfig(final MainActivity p)
	{
		Globals.TrackballDampening = 0;
		if( ! Globals.PhoneHasTrackball )
		{
			showAdditionalInputConfig(p);
			return;
		}
		
		final CharSequence[] items = {"No dampening", "Fast", "Medium", "Slow"};

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle("Trackball dampening");
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.TrackballDampening = item;

				dialog.dismiss();
				showAdditionalInputConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}
	
	static void showAdditionalInputConfig(final MainActivity p)
	{
		if( ! Globals.AppNeedsArrowKeys )
		{
			showAccelerometerConfig(p);
			return;
		}
		final CharSequence[] items = {
			"On-screen keyboard" /* + ( Globals.AppUsesMouse ? " (disables mouse input)" : "") */ ,
			"Accelerometer as navigation keys" /* + ( Globals.AppUsesJoystick ? " (disables joystick input)" : "" ) */ ,
			"Both accelerometer and on-screen keyboard",
			"No additional controls"
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle("Additional controls to use");
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.UseTouchscreenKeyboard = (item == 0 || item == 2);
				Globals.UseAccelerometerAsArrowKeys = (item == 1 || item == 2);

				dialog.dismiss();
				showAccelerometerConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}

	static void showAccelerometerConfig(final MainActivity p)
	{
		Globals.AccelerometerSensitivity = 0;
		if( ! Globals.UseAccelerometerAsArrowKeys )
		{
			showScreenKeyboardConfig(p);
			return;
		}
		
		final CharSequence[] items = {"Fast", "Medium", "Slow"};

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle("Accelerometer sensitivity");
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.AccelerometerSensitivity = item;

				dialog.dismiss();
				showScreenKeyboardConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}

	static void showScreenKeyboardConfig(final MainActivity p)
	{
		Globals.TouchscreenKeyboardSize = 0;
		if( ! Globals.UseTouchscreenKeyboard )
		{
			showScreenKeyboardThemeConfig(p);
			return;
		}
		
		final CharSequence[] items = {"Big", "Medium", "Small", "Tiny"};

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle("On-screen keyboard size (toggle auto-fire by sliding across Fire button)");
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.TouchscreenKeyboardSize = item;

				dialog.dismiss();
				showScreenKeyboardThemeConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}

	static void showScreenKeyboardThemeConfig(final MainActivity p)
	{
		Globals.TouchscreenKeyboardTheme = 0;
		if( ! Globals.UseTouchscreenKeyboard )
		{
			showAudioConfig(p);
			return;
		}
		
		final CharSequence[] items = {"Ultimate Droid by Sean Stieber", "Ugly Arrows by pelya"};

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle("On-screen keyboard theme");
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				if( item == 0 )
					Globals.TouchscreenKeyboardTheme = 1;
				if( item == 1 )
					Globals.TouchscreenKeyboardTheme = 0;

				dialog.dismiss();
				showAudioConfig(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}
	
	static void showAudioConfig(final MainActivity p)
	{
		final CharSequence[] items = {"Small (fast devices)", "Medium", "Large (if sound is choppy)"};

		AlertDialog.Builder builder = new AlertDialog.Builder(p);
		builder.setTitle("Size of audio buffer");
		builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface dialog, int item) 
			{
				Globals.AudioBufferConfig = item;
				dialog.dismiss();
				Save(p);
				startDownloader(p);
			}
		});
		AlertDialog alert = builder.create();
		alert.setOwnerActivity(p);
		alert.show();
	}

	static void Apply(Activity p)
	{
		nativeIsSdcardUsed( Globals.DownloadToSdcard ? 1 : 0 );
		
		if( Globals.PhoneHasTrackball )
			nativeSetTrackballUsed();
		if( Globals.AppUsesMouse )
			nativeSetMouseUsed();
		if( Globals.AppUsesJoystick && !Globals.UseAccelerometerAsArrowKeys )
			nativeSetJoystickUsed();
		if( Globals.AppUsesMultitouch )
			nativeSetMultitouchUsed();
		nativeSetAccelerometerSensitivity(Globals.AccelerometerSensitivity);
		nativeSetTrackballDampening(Globals.TrackballDampening);
		if( Globals.UseTouchscreenKeyboard )
		{
			nativeSetTouchscreenKeyboardUsed();
			nativeSetupScreenKeyboard(	Globals.TouchscreenKeyboardSize, 
										Globals.TouchscreenKeyboardTheme,
										Globals.AppTouchscreenKeyboardKeysAmount, 
										Globals.AppTouchscreenKeyboardKeysAmountAutoFire);
		}
		SetupTouchscreenKeyboardGraphics(p);
		String lang = new String(Locale.getDefault().getLanguage());
		if( Locale.getDefault().getCountry().length() > 0 )
			lang = lang + "_" + Locale.getDefault().getCountry();
		System.out.println( "libSDL: setting envvar LANG to '" + lang + "'");
		nativeSetEnv( "LANG", lang );
		// TODO: get current user name and set envvar USER, the API is not availalbe on Android 1.6 so I don't bother with this
	}

	static byte [] loadRaw(Activity p,int res)
	{
		byte [] buf = new byte[128];
		byte [] a = new byte[0];
		try{
			InputStream is = new GZIPInputStream(p.getResources().openRawResource(res));
			int readed = 0;
			while( (readed = is.read(buf)) >= 0 )
			{
				byte [] b = new byte[a.length + readed];
				for(int i = 0; i < a.length; i++)
					b[i] = a[i];
				for(int i = 0; i < readed; i++)
					b[i+a.length] = buf[i];
				a = b;
			}
		} catch(Exception e) {};
		return a;
	}
	
	static void SetupTouchscreenKeyboardGraphics(Activity p)
	{
		if( Globals.UseTouchscreenKeyboard )
		{
			if( Globals.TouchscreenKeyboardTheme == 1 )
			{
				// DPAD
				nativeSetupScreenKeyboardButton(0,  loadRaw(p, R.raw.ultimatedroiddpadbutton));
				nativeSetupScreenKeyboardButton(1,  loadRaw(p, R.raw.ultimatedroidleftbuttonpressed));
				nativeSetupScreenKeyboardButton(2,  loadRaw(p, R.raw.ultimatedroidrightbuttonpressed));
				nativeSetupScreenKeyboardButton(3,  loadRaw(p, R.raw.ultimatedroidupbuttonpressed));
				nativeSetupScreenKeyboardButton(4,  loadRaw(p, R.raw.ultimatedroiddownbuttonpressed));
				// Auto-fire
				nativeSetupScreenKeyboardButton(5,  loadRaw(p, R.raw.ultimatedroidbutton1auto));
				nativeSetupScreenKeyboardButton(6,  loadRaw(p, R.raw.ultimatedroidbutton1autoanim));
				nativeSetupScreenKeyboardButton(7,  loadRaw(p, R.raw.ultimatedroidbutton2auto));
				nativeSetupScreenKeyboardButton(8,  loadRaw(p, R.raw.ultimatedroidbutton2autoanim));
				// Other buttons
				nativeSetupScreenKeyboardButton(9,  loadRaw(p, R.raw.ultimatedroidbutton1));
				nativeSetupScreenKeyboardButton(10, loadRaw(p, R.raw.ultimatedroidbutton1pressed));
				nativeSetupScreenKeyboardButton(11, loadRaw(p, R.raw.ultimatedroidbutton2));
				nativeSetupScreenKeyboardButton(12, loadRaw(p, R.raw.ultimatedroidbutton2pressed));
				nativeSetupScreenKeyboardButton(13, loadRaw(p, R.raw.ultimatedroidbutton3));
				nativeSetupScreenKeyboardButton(14, loadRaw(p, R.raw.ultimatedroidbutton3pressed));
				nativeSetupScreenKeyboardButton(15, loadRaw(p, R.raw.ultimatedroidbutton4));
				nativeSetupScreenKeyboardButton(16, loadRaw(p, R.raw.ultimatedroidbutton4pressed));
				nativeSetupScreenKeyboardButton(17, loadRaw(p, R.raw.ultimatedroidbutton5));
				nativeSetupScreenKeyboardButton(18, loadRaw(p, R.raw.ultimatedroidbutton5pressed));
				nativeSetupScreenKeyboardButton(19, loadRaw(p, R.raw.ultimatedroidbutton6));
				nativeSetupScreenKeyboardButton(20, loadRaw(p, R.raw.ultimatedroidbutton6pressed));
				nativeSetupScreenKeyboardButton(21, loadRaw(p, R.raw.ultimatedroidbutton7));
				nativeSetupScreenKeyboardButton(22, loadRaw(p, R.raw.ultimatedroidbutton7));
			}
		}
	}
	
	static void startDownloader(MainActivity p)
	{
		class Callback implements Runnable
		{
			public MainActivity Parent;
			public void run()
			{
				Parent.startDownloader();
			}
		}
		Callback cb = new Callback();
		cb.Parent = p;
		p.runOnUiThread(cb);
	};
	

	private static native void nativeIsSdcardUsed(int flag);
	private static native void nativeSetTrackballUsed();
	private static native void nativeSetTrackballDampening(int value);
	private static native void nativeSetAccelerometerSensitivity(int value);
	private static native void nativeSetMouseUsed();
	private static native void nativeSetJoystickUsed();
	private static native void nativeSetMultitouchUsed();
	private static native void nativeSetTouchscreenKeyboardUsed();
	private static native void nativeSetupScreenKeyboard(int size, int theme, int nbuttons, int nbuttonsAutoFire);
	private static native void nativeSetupScreenKeyboardButton(int buttonId, byte[] img);
	public static native void nativeSetEnv(final String name, final String value);
}

