# AutoCalendarAlarms

Small Android app: pick which calendars (categories) you care about and it
automatically sets an alarm before those events start, so you don't have to
set reminders by hand every time.

## Using it

You'll need an Android device or emulator with some calendars synced to it
(a Google account works fine). There's no Play Store listing, but there's a
built apk on the [releases page](https://github.com/ame38/AutoCalendarAlarms/releases).

To actually get it on your phone: open that releases page in your phone's
browser, download the apk, then open it from your downloads/notifications
to install. Android will probably block it the first time and ask you to
allow installs from that source (Chrome, Files, whatever you used) — say
yes, then go back and open the download again to install it. If you'd
rather use adb, `adb install AutoCalendarAlarms-0.0.0.apk` works too once
it's on your machine.

Or just clone the repo and run it from Android Studio yourself.

First launch it'll ask for calendar read permission (to see your events) and
notification permission (to actually show you the alarm). Both are needed
for it to do anything useful.

From there:

1. On the main screen, check off the calendars you actually want alarms for.
2. Pick how far ahead you want to be warned (5/15/30/60 min, whatever suits you).
3. Tap "view upcoming events" to see what's coming up and have alarms scheduled for them.
4. Just leave it be — you'll get a notification before each event starts. There's a refresh button on the events screen if you add stuff to your calendar and want it picked up right away, and alarms should survive a reboot too.

## License

Apache 2.0, see LICENSE.
