# diSPIMFusionUI
ImageJ UI for cuda-based diSPIM fusion code

Uses MigLayout to generate a dialog, stores these in Java Preferencese for later re-use of settings, and is fully ImageJ-macro scriptable. Currently only works with image files stored on disk (as the actual work is done by a C++ program that is only available on Windows), i.e. this is really only a UI to set up all the parameters.
