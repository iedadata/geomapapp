# GeoMapApp
Java application for exploring geoscience data

# Overview
GeoMapApp is an earth science exploration and visualization application that is continually being expanded as part of the Marine Geoscience Data System (MGDS) at the Lamont-Doherty Earth Observatory of Columbia University. The application provides direct access to the Global Multi-Resolution Topography (GMRT) compilation that hosts high resolution (~100 m node spacing) bathymetry from multibeam data for ocean areas and ASTER (Advanced Spaceborne Thermal Emission and Reflection Radiometer) and NED (National Elevation Dataset) topography datasets for the global land masses.

This code base also includes the code for Virtual Ocean. Virtual Ocean integrates the GeoMapApp tool suite with the NASA World Wind 3-D earth browser to create a powerful new platform for interdisciplinary research and education.

The code is implemented in Java.

# Requirements
* Java 8 or higher.
* ant.

# Installation
The instructions for setting up the code for GeoMapApp for development can be found here:
http://wiki.iedadata.org/display/GFG/How+to+build+GeoMapApp+with+PetDB+Portal+from+source+code

# Building and Releasing GeoMapApp
The instructions for building and releasing GeoMapApp can be found here:
http://wiki.iedadata.org/display/GFG/How+to+build+and+release+GeoMapApp

# What's here

This section provides a brief description of the source code files and folders in this repository:

## .gitignore
A file which contains a list of files and directories that Git has been explicitly told to ignore. 

## LICENSE
The Apache Lisense file for this product.

## README.md
This file.

## build.xml
The build file used to create the signed Unix and Windows jar files.

## buildUnsigned.xml
The build file used to create the unsigned Apple jar files.

## buildww.xml
The build file used to create the Virtual Ocean jar file.

## javadoc.xml
The javadoc file for Virtual Ocean.

## haxby/ and org/
Directories that contain the Java source code for GeoMapApp and Virtual Ocean.

## resources/
A directory that contains the various resources required by GeoMapApp and Virtual Ocean, e.g. external libraries, images, icons, color look up tables, etc.
