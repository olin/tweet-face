# TWEET FACE

Tweets something, and attaches a picture of your face at the time of the tweet.

## Building

In case you want to build this...

  1. clone this library in an adjacent folder: ```git@github.com:ohack/ActionBarSherlock.git```
  1. add the ActionBarSherlock folder as a library project (ask Mark if you can't figure this out)
  1. build it against the Android 3.2 platform SDK

## Specifics

  * Uses ActionBarSherlock to show cool top bar with icon and menu options
  * Forces OAuth login on Tweet and Login actionbar item
  * Tweet character count is 119 because of space + 20-char t.co link
  * Uses Twitter4J to perform OAuth and Twitter update with media
  * Camera picture is set to smallest size for fast upload
  * Camera preview is a 0dp x 0dp surface (invisible)
  * OAuth tokens stored in shared preferences