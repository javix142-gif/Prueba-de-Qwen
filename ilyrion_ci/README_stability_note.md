# Ilyrion stability candidate

This candidate deliberately removes direct Android 30/33 window/back class references from startup, opts out of predictive back temporarily on targetSdk 36, preserves the responsive web UI, and shows an in-app diagnostic screen instead of allowing an uncaught startup exception to terminate the process.
