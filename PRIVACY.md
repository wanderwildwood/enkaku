# Privacy

Remote has no internet permission. It cannot send anything anywhere but over Bluetooth.

**Over Bluetooth** it talks only to the device you pair it with, as a remote, a keyboard and a
mouse. What it sends is what you press and type: button presses, key presses, pointer
movement, and the text you send from the keyboard panel.

**On the phone** it keeps its settings, the devices you marked as favourites and the one it
connects to on its own. Nothing else is stored. Uninstalling it removes all of that.

**Permissions:** Bluetooth, to pair and connect; a foreground service, so the connection holds
while the screen is off; and notifications, for the buttons it can put in the notification
shade. On Android 11 and older, location is asked for too, because those versions require it
to search for Bluetooth devices. It is never used for anything else.

Four more are added by Android's own libraries and are never asked for. WorkManager, which
the home-screen remote widget runs on, brings a wake lock, starting at
boot, and seeing whether the phone is online (its check, for jobs that would need the internet;
Remote has none). The support library adds `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which keeps
the app's own internal messages to itself. None of them sends anything anywhere.

**Backups:** Android does not back up Remote's settings or devices; they stay on the phone.
