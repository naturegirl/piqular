#!/bin/bash

# export launcher icons into the folder they belong to
# Android launcher icon sizes:
# ldpi: 32 x 32
# mdpi: 48 x 48
# hdpi: 72 x 72
# xhdpi: 96 x 96
# xxhdpi: 144 x 144

cp -v icon32.png ../res/drawable-ldpi/ic_launcher.png
cp -v icon48.png ../res/drawable-mdpi/ic_launcher.png
cp -v icon72.png ../res/drawable-hdpi/ic_launcher.png
cp -v icon96.png ../res/drawable-xhdpi/ic_launcher.png
cp -v icon144.png ../res/drawable-xxhdpi/ic_launcher.png