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

**On the phone** it keeps its settings, the devices you marked as favorites and the one it
connects to on its own, and the last Kodi it reached: its name, address, and the username and
password you gave it. Nothing else is stored. Uninstalling it removes all of that, and
"Forget" on the Kodi screen removes the Kodi. **Save the Kodi box to a file** (Settings) writes
its name, address, port and username, never the password, to one file you choose through the
phone's own file picker, and only when you ask; **Restore** reads such a file the same way and
checks all of it before it changes anything.

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

<!-- shared-about privacy section -->
## Who we are

Remote is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. What you put in Remote stays on your phone. The only things that go out are the ones described above: your button presses, key presses, pointer movement and typed text, to the device you pair over Bluetooth or to the Kodi you choose (with its login, if it asks for one). The developer receives nothing and holds no data about you.

## How to contact us

Write to email@wanderthe.dev about your data, this policy or anything else. The same address is in the app, under About.

## Your rights

If you are in the EU, the EEA or the UK, the GDPR gives you the right to:

- see the personal data held about you (access)
- have it corrected (rectification) or deleted (erasure)
- limit how it is used (restriction)
- object to its use (objection)
- get a copy to take elsewhere (portability)
- withdraw your consent at any time, where something rests on it
- complain to a data protection supervisory authority, for example the one where you live

Similar laws elsewhere, such as the CCPA in California, give you the right to know, delete and correct, and to opt out of the sale or sharing of your data. Nothing from Remote is sold or shared.

Since Remote keeps what it stores only on your phone, you can exercise most of these rights yourself: use Forget on the Kodi screen to remove the Kodi and its login, clear the app's data in Android's settings, or uninstall the app, which deletes all of it. For anything you cannot do yourself, write to the address above.

If you point Remote at a server of your own, what that server keeps is yours to manage.
<!-- /shared-about privacy section -->
