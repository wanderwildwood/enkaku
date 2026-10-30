# Privacy

Remote talks to two things only: the device you pair it with over Bluetooth, and, if you use
it, the Kodi you choose on your home Wi-Fi. It sends nothing anywhere else.

**Over Bluetooth** it talks only to the device you pair it with, as a remote, a keyboard and a
mouse. What it sends is what you press and type: button presses, key presses, pointer
movement, and the text you send from the keyboard panel.

**Over Wi-Fi** it can drive Kodi instead. It then sends Kodi the button presses
and the text you send from the keyboard panel, as JSON-RPC over plain HTTP (Kodi offers
nothing else), to the one address you chose, and only while its remote is open. If Kodi asks
for a username and password, those go with each press, to that address and no other. To find
Kodi boxes it listens on the home network for the ones that announce themselves (mDNS,
through Android's own service discovery); it announces nothing about itself.

**The Kodi login** (username and password) is sent only to the Kodi address you entered, and over plain HTTP, because Kodi offers nothing else. Anyone on that network who can see its traffic could read it, so use it on a network you trust. The app refuses to send it to any address that is not on a private network (192.168.x.x, 10.x.x.x, 172.16-31.x.x, link-local, or a Tailscale-style 100.64.x.x address): nothing is sent to such an address, and the app says so. On the phone it is kept sealed with a key held in the phone's keystore, not as plain text.

**On the phone** it keeps its settings, the devices you marked as favourites and the one it
connects to on its own, and the last Kodi it reached: its name, address, and the username and
password you gave it. Nothing else is stored. Uninstalling it removes all of that, and
"Forget" on the Kodi screen removes the Kodi.

**Terms:** Remote is free software under the GNU General Public License v3 or later. It comes with no warranty: use it at your own risk.

**Permissions:** Bluetooth, to pair and connect; internet, for Kodi over Wi-Fi, used for
nothing but the Kodi address you choose; a foreground service, so the connection holds
while the screen is off; and notifications, for the buttons it can put in the notification
shade. On Android 11 and older, location is asked for too, because those versions require it
to search for Bluetooth devices. It is never used for anything else.

Four more are added by Android's own libraries and are never asked for. WorkManager, which
the home-screen remote widget runs on, brings a wake lock, starting at
boot, and seeing whether the phone is online (its check, for jobs that would need the internet;
Remote runs none). The support library adds `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which keeps
the app's own internal messages to itself. None of them sends anything anywhere.

**Backups:** Android does not back up Remote's settings or devices; they stay on the phone.
