ARVisionMap
=========== 
###An User Interface Framework for Augmented Reality Assisted Indoor Navigation Systems
####Authors: Nahush Bhanage, Chun-Yuan Yang, Andrew Zhong

###Introduction
In this project, we incorporated augmented reality into indoor positioning and designed a novel user interface with a compelling application that delivers an interactive indoor navigation experience through augmented graphical views aligned with indoor objects. It is demonstrated in the CITRIS Invention Lab at Berkeley and works by providing augmented navigation, interactive device instructions, demo products display and device reservation lookup for lab visitors and apprentices. It runs on the Android mobile platform powered by OpenGL graphics, orientation sensors and an indoor position simulator. The user study shows that 80% of users prefer our augmented reality based user interface.

###Navigation mode
A user can select his or her device of interest from a dropdown menu. An augmented arrow powered by OpenGL graphics will navigate to that specific device. The direction vector is computed by subtracting location coordinates of the phone or tablet from those of the device of interest.<br>
<br><img src="http://www.ocf.berkeley.edu/~andrewxz/pics/Picture1.png">

###Apprentice mode
Apprentice mode will start with a set of step-by-step instructions. The instructions are in swipeable slides and implemented with Android ViewPager. <br>
<br><img src="http://www.ocf.berkeley.edu/~andrewxz/pics/Picture4.png">

###Visitor mode
This mode will render previous students’ works on this device. In the figure below, a 3D model of a dinosaur was rendered in visitor mode of “Afinia H-series”, the 3D printer. Users can also rotate, zoom in and zoom out the model to get the best viewing angle.<br>
<br><img src="http://www.ocf.berkeley.edu/~andrewxz/pics/Picture3.png">

###Calendar mode
There is also a calendar mode where users can check the reservation status of all devices. As shown in the figure, augmented arrows are aligned with each device and their colors notify the availability. By touching a device on the camera view, users will be directed to the calendar webpage for that device.<br>
<br><img src="http://www.ocf.berkeley.edu/~andrewxz/pics/Picture2.png">

#####Reference
Zhong, Andrew. Improving User Experiences in Indoor Navigation with Augmented Reality. Technical Report No. UCB/EECS-2014-74. EECS Department, U of California, Berkeley. 
