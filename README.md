# GeoMapApp
Java application for exploring geoscience data.

# Overview
GeoMapApp is an earth science exploration and visualization application that is continually being expanded as part of the Marine Geoscience Data System (MGDS) at the Lamont-Doherty Earth Observatory of Columbia University. The application provides direct access to the Global Multi-Resolution Topography (GMRT) compilation that hosts high resolution (~100 m node spacing) bathymetry from multibeam data for ocean areas and ASTER (Advanced Spaceborne Thermal Emission and Reflection Radiometer) and NED (National Elevation Dataset) topography datasets for the global land masses.

This code base also includes the code for Virtual Ocean. Virtual Ocean integrates the GeoMapApp tool suite with the NASA World Wind 3-D earth browser to create a powerful new platform for interdisciplinary research and education.

The code is implemented in Java.

# Requirements
*  Java SE Development Kit 8 or higher.
*	Developers must have convenient access to the source code as well as modern IDE (Integrated Development Environment) tools to easily participate in the project’s life-cycle. Concurrent editing of the same source should be naturally supported.
*	The project leader must have robust, industry-standard versioning tools to manage the incoming code from contributors (accept, revert & modify changes). 
*	To ensure consistent build releases, the project leader must have tools to mark existing code (at any given time) as alpha, beta or release candidate. 
*	The project leader must have tools to define user permissions for source code access and/or modification. 
*	The project leader must guide the development team by publishing:
   	* Desired Features Specifications
    * UML, Use Case & Flow Control diagrams
    *	Known Application Bugs

# Installation and Development Environment Setup
The instructions for setting up the code for GeoMapApp for development can be found here:
http://wiki.iedadata.org/download/attachments/14582856/GMA_and_Eclipse.doc

# Building and Releasing GeoMapApp
The instructions for building and releasing GeoMapApp for Unix, Windows and Apple OS X can be found here:
http://wiki.iedadata.org/display/GFG/How+to+build+and+release+GeoMapApp

# Release History
* 07/20/2017 v3.6.6 Public release of v3.6.6 of GeoMapApp.
* 04/17/2018 v3.6.8 Public release of v3.6.8 of GeoMapApp.

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
