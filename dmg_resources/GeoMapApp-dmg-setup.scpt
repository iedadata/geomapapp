tell application "Finder"
  set theDisk to a reference to (disks whose URL = "file:///Users/Neville/eclipse-workspace/GeoMapApp/dmg_temp/images/GeoMapApp/")
  open theDisk

  set theWindow to a reference to (container window of disks whose URL = "file:///Users/Neville/eclipse-workspace/GeoMapApp/dmg_temp/images/GeoMapApp/")

  set current view of theWindow to icon view
  set toolbar visible of theWindow to false
  set statusbar visible of theWindow to false

  -- size of window should fit the size of background
  set the bounds of theWindow to {400, 90, 1005, 440}

  set theViewOptions to a reference to the icon view options of theWindow
  set arrangement of theViewOptions to not arranged
  set icon size of theViewOptions to 120
  set background picture of theViewOptions to POSIX file "/Users/Neville/eclipse-workspace/GeoMapApp/dmg_temp/images/GeoMapApp/.background/background.tiff"

  -- Create alias for install location
  make new alias file at POSIX file "/Users/Neville/eclipse-workspace/GeoMapApp/dmg_temp/images/GeoMapApp" to POSIX file "/Applications" with properties {name:"/Applications"}

  set allTheFiles to the name of every item of theWindow
  repeat with theFile in allTheFiles
    set theFilePath to POSIX path of theFile
    if theFilePath is "/Applications" then
      -- Position install location
      set position of item theFile of theWindow to {510, 60}
    else
      -- Position application or runtime
      set position of item theFile of theWindow to {100, 60}
    end if
  end repeat

  update theDisk without registering applications
  delay 5
  close (get window of theDisk)
end tell
