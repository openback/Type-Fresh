Type Fresh
==========

*An Android app that allows users to change their system fonts.*

Programmed by: *openback*&lt;[openback@pixelpod.net][1]&gt;
 
Art by: *minusbaby* &lt;[hello@minusbaby.com][2]&gt;

The future home of Tiny Fresh will be at [Pixelpod.net][3]

Description
-----------

This app is for **systems with root access only**. It offers 3 main functions:

  - Apply selected TTF fonts to the system
  - Backup system fonts to `/sdcard/Fonts`
  - Restore backed up fonts from `/sdcard/Fonts`

Place any TTF files you have in `/sdcard/Fonts` for easiest access.

In order for Android to reload its system fonts, the system must be rebooted after applying. Also, most ROM updates will overwrite your fonts with the default ones while installing. To easily restore, simply find a set of fonts that work well for you and then select `Backup fonts` so that you can restore them later. Be aware that this simply overwrites any existing backup you have, so you *may* want to save the original fonts elsewhere if you think you may need them.

Note
----
DroidSansFallback.ttf is the font that contains most of the international characters. Make sure that you use a font with more than just Latin characters. A good replacement seems to be [DejaVu][4], which contains even more characters from more languages than Android supports already as noted on [this XDA forums thread][5]


  [1]: mailto:openback@pixelpod.net
  [2]: mailto:hello@minusbaby.com
  [3]: http://pixelpod.net
  [4]: http://dejavu-fonts.org/
  [5]: http://forum.xda-developers.com/showthread.php?t=480964
