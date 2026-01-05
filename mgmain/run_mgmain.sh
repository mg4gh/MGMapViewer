#!/bin/bash

# Navigate to the module directory
MODULE_DIR="$(dirname "$0")"
cd "$MODULE_DIR"

# Build the classpath
# Includes:
# 1. The compiled classes
# 2. The certs directory (since resources are loaded from classpath root)
# 3. All dependency JARs in build/dependencies
CP="build/classes/java/main:certs:build/dependencies/*"

# Run java
java -cp "$CP" de.soft4mg.mgmain.MgMain
