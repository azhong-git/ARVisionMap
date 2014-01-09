Augmented Object Detector / Viewer on Android
----------------------------------------------
###Introduction
In this project, we explored an application of augmented reality by creating an Android app that is able to recognize trained objects from the camera video stream and render an aligned 3D model of the object in the screen virtual space. Furthermore, the location and orientation of either the camera or rendered object can be easily maneuvered by swiping the screen, rotating or moving the camera. With all these features, the user is able to scan an object (a real object or an image), move, rotate, pinpoint its 3D model to a geographical location as well as walk into an augmented virtual scene. 
<br>
<img src="http://www.ocf.berkeley.edu/~andrewxz/pics/androidAR.jpg">

###OpenGL ES 2.0
The user interface of our application consists of an OpenGL rendered graphics view overlaid on top of the live camera stream. We have coded multiple programmable vertex and fragment shaders in GLSL. These shaders handle model, view and projection transformations on vertices and texture mapping on object faces.

###Object Detection
The real time object detection from the camera video stream has to be fast. We have trained a cascade of boosted Haar feature classifiers because of the constant calculation time of Haar-like features. The training took over 19 hours to complete on over 300 samples images in 12 stages. 

###Objects Rendering
We have managed to convert each Wavefront .obj file to a header file containing the number of triangle faces, vertex, normal and texture coordinates with a Python script. When an object is created in the Android renderer program, its header file will be read into a native array specifically allocated for that object. We also added support for simple non-photorealistic rendering of textures with arbitrary transparency and a small discrete palette.

###Orientation sensors
We extracted the orientation from the accelerator and magnetometer of the phone in real time. The main program activity listens to sensor change events and during each event, it records the azimuth, pitch and roll angles of the phone and remap the rotation matrix to the most recent coordinate system. Furthermore, we added a low-pass filter to stabilize the sensor outputs. We then updated the view matrix (specifically lookat location, camera up and camera right vector) according to the three filtered angles, so that only the region that the camera is pointing at is displayed on the screen.  We have also rendered a compass (texture mapped onto a rectangular object) that work in conjunction with the orientation sensors and points to the magnetic north. 

###Touch events
We have registered callbacks on touch events in the Android framework. Currently we handle two type of touch - drag to zoom in/out and long press to move objects. If the user drags on the screen, we obtain the drag velocity (from the Android framework) and accordingly modify the view matrix to zoom in/out of the scene. On long press, we convert the screen touch coordinates (obtained from the Android framework) to world coordinates and apply transformations accordingly.

###Location services and virtual augmented viewing
We have added support for virtual viewing by changing camera and object locations. The camera location can be updated manually by swiping the screen or automatically by GPS/mobile networks. The original object location is computed based on a pre-configured mapping from 2D screen location together with phone orientation to a 3D virtual coordinate. With this, the 3D object model is able to align with and pop up on the virtual space where it gets detected.

Dec 2013
