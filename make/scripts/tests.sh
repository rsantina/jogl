#! /bin/bash

if [ -z "$1" -o -z "$2" ] ; then
    echo Usage $0 java-exe build-dir
    exit 0
fi

javaexe=$1
shift
bdir=$1
shift

if [ ! -x $javaexe ] ; then
    echo java-exe $javaexe is not an executable
    exit 1
fi
if [ ! -d $bdir ] ; then
    echo build-dir $bdir is not a directory
    exit 1
fi

rm -f java-run.log

spath=`dirname $0`

. $spath/setenv-jogl.sh $bdir JOGL_ALL

MOSX=0
MOSX_MT=0
uname -a | grep -i Darwin && MOSX=1
if [ $MOSX -eq 1 ] ; then
    MOSX_MT=1
fi

which $javaexe 2>&1 | tee -a java-run.log
$javaexe -version 2>&1 | tee -a java-run.log
echo LIBXCB_ALLOW_SLOPPY_LOCK: $LIBXCB_ALLOW_SLOPPY_LOCK 2>&1 | tee -a java-run.log
echo LIBGL_DRIVERS_PATH: $LIBGL_DRIVERS_PATH 2>&1 | tee -a java-run.log
echo LIBGL_DEBUG: $LIBGL_DEBUG 2>&1 | tee -a java-run.log
echo SWT_CLASSPATH: $SWT_CLASSPATH 2>&1 | tee -a java-run.log
echo $javaexe $X_ARGS $D_ARGS $* 2>&1 | tee -a java-run.log
echo CLASSPATH $CLASSPATH 2>&1 | tee -a java-run.log
echo MacOsX $MOSX

function jrun() {
    awton=$1
    shift

    #D_ARGS="-Djogl.debug.ExtensionAvailabilityCache -Djogl.debug=all -Dnativewindow.debug=all -Djogamp.debug.ProcAddressHelper=true -Djogamp.debug.NativeLibrary=true -Djogamp.debug.NativeLibrary.Lookup=true"
    #D_ARGS="-Djogl.debug=all -Dnativewindow.debug=all"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.ExtensionAvailabilityCache"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GLProfile -Djogl.debug.GLDrawable"
    #D_ARGS="-Djogl.debug.GLProfile"
    # D_ARGS="-Dnewt.debug.EDT -Dnativewindow.debug.ToolkitLock.TraceLock -Dnativewindow.debug.NativeWindow"
    #D_ARGS="-Dnewt.debug.Window -Dnewt.debug.Display -Dnewt.debug.EDT"
    #D_ARGS="-Dnewt.debug.EDT -Dnewt.debug.Window -Djogl.debug.GLContext"
    #D_ARGS="-Dnativewindow.debug.ToolkitLock.TraceLock -Dnativewindow.debug.X11Util.TraceDisplayLifecycle=true -Dnativewindow.debug.X11Util"
    #D_ARGS="-Dnativewindow.debug.X11Util -Djogl.debug.GLContext -Djogl.debug.GLDrawable -Dnewt.debug=all"
    #D_ARGS="-Dnativewindow.debug.X11Util"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=600000 -Djogamp.debug.Lock -Djogamp.debug.Lock.TraceLock"
    #D_ARGS="-Djogamp.common.utils.locks.Lock.timeout=1000 -Djogamp.debug.Lock -Djogamp.debug.Lock.TraceLock"
    #D_ARGS="-Dnewt.debug.EDT -Djogamp.common.utils.locks.Lock.timeout=600000 -Djogl.debug.Animator -Dnewt.debug.Display -Dnewt.debug.Screen"
    #D_ARGS="-Dnewt.debug.Window -Djogamp.common.utils.locks.Lock.timeout=600000 -Djogl.debug.Animator"
    #D_ARGS="-Djogl.debug.Animator -Dnewt.debug=all"
    #D_ARGS="-Dnewt.debug.EDT -Dnewt.debug.Display -Dnativewindow.debug.X11Util -Djogl.debug.GLDrawable -Djogl.debug.GLCanvas"
    #D_ARGS="-Djogl.debug.GLContext -Dnewt.debug=all"
    #D_ARGS="-Djogl.debug.GraphicsConfiguration -Djogl.debug.CapabilitiesChooser"
    #D_ARGS="-Dnewt.debug.Screen -Dnewt.debug.EDT -Djogamp.debug.Lock"
    #D_ARGS="-Djogl.debug.GLContext -Djogl.debug.GraphicsConfiguration"
    #D_ARGS="-Dnewt.debug.EDT"
    #D_ARGS="-Djogl.debug=all -Dnativewindow.debug=all -Dnewt.debug=all"
    #D_ARGS="-Djogl.debug=all -Dnewt.debug=all"
    #D_ARGS="-Dnewt.debug.Window -Dnewt.debug.Display -Dnewt.debug.EDT -Djogl.debug.GLContext"
    #D_ARGS="-Dnewt.debug.Window -Djogl.debug.Animator -Dnewt.debug.Screen"
    #D_ARGS="-Dnewt.debug.Window"
    #D_ARGS="-Xprof"
    #D_ARGS="-Djogl.debug.Animator"
    #D_ARGS="-Dnativewindow.debug=all"
    #D_ARGS="-Djogl.debug.GraphicsConfiguration"
    #D_ARGS="-Djogl.debug.GLCanvas -Djogl.debug.GraphicsConfiguration"
    #D_ARGS="-Djogl.debug.GLCanvas"
    #D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.GLDebugMessageHandler -Djogl.debug.GLSLCode"
    #D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL"
    #D_ARGS="-Djogl.debug.GLDebugMessageHandler -Dnewt.debug.Window -Dnewt.debug.Display -Dnewt.debug.EDT"
    #D_ARGS="-Djogl.debug.GLDebugMessageHandler"
    #D_ARGS="-Djogl.debug.GLDebugMessageHandler -Djogl.debug.DebugGL"
    #D_ARGS="-Djogl.debug.GLDebugMessageHandler -Djogl.debug.TraceGL -Djogl.debug.DebugGL -Djogl.debug.GLSLCode -Djogl.debug.GLSLState"
    #D_ARGS="-Djogl.debug.GLDebugMessageHandler -Djogl.debug.DebugGL -Djogl.debug.TraceGL"
    #D_ARGS="-Djogl.debug.DebugGL -Djogl.debug.TraceGL"
    #D_ARGS="-Dnativewindow.debug.ToolkitLock.TraceLock"
    #D_ARGS="-Djogl.debug.graph.curve -Djogl.debug.GLSLCode -Djogl.debug.TraceGL"
    #D_ARGS="-Djogl.debug.graph.curve -Djogl.debug.GLSLState"
    #D_ARGS="-Djogamp.debug.JARUtil"
    #D_ARGS="-Djogamp.debug.TempFileCache"
    #D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.debug.TempFileCache -Djogamp.debug.JARUtil"
    #D_ARGS="-Djogamp.debug.JNILibLoader"
    #D_ARGS="-Djogamp.debug.JNILibLoader -Djogamp.gluegen.UseTempJarCache=false -Djogamp.debug.JARUtil"
    #D_ARGS="-Dnewt.test.EDTMainThread -Dnewt.debug.MainThread"
    #C_ARG="com.jogamp.newt.util.MainThread"
    #D_ARGS="-Dnewt.debug.MainThread"
    #D_ARGS="-Dnewt.debug=all -Djogamp.debug.Lock.TraceLock -Djogamp.common.utils.locks.Lock.timeout=600000"
    #D_ARGS="-Dnewt.debug=all -Djogamp.debug.Lock -Djogamp.debug.Lock.TraceLock"
    #D_ARGS="-Djogl.debug.GLContext -Dnewt.debug=all -Djogamp.debug.Lock -Djogamp.common.utils.locks.Lock.timeout=10000"
    #D_ARGS="-Dnewt.debug=all"
    #X_ARGS="-Dsun.java2d.noddraw=true -Dsun.java2d.opengl=true"
    #X_ARGS="-verbose:jni"

    if [ $awton -eq 1 ] ; then
        X_ARGS="-Djava.awt.headless=false"
    else
        X_ARGS="-Djava.awt.headless=true"
    fi
    if [ $MOSX_MT -eq 1 ] ; then
        X_ARGS="-XstartOnFirstThread $X_ARGS"
        C_ARG="com.jogamp.newt.util.MainThread"
    fi
    echo
    echo "Test Start: $*"
    echo
    echo LD_LIBRARY_PATH $LD_LIBRARY_PATH
    echo
    echo $javaexe $X_ARGS $D_ARGS $C_ARG $*
    #LD_LIBRARY_PATH=/usr/local/projects/Xorg.modular/build-x86_64/lib:$LD_LIBRARY_PATH \
    #LD_LIBRARY_PATH=/opt-linux-x86_64/x11lib-1.3:$LD_LIBRARY_PATH \
    #LD_LIBRARY_PATH=/opt-linux-x86_64/mesa-7.8.1/lib64:$LD_LIBRARY_PATH \
    #LIBGL_DRIVERS_PATH=/usr/lib/mesa:/usr/lib32/mesa \
    #LD_LIBRARY_PATH=/usr/lib/mesa:/usr/lib32/mesa:$LD_LIBRARY_PATH \
    $javaexe $X_ARGS $D_ARGS $C_ARG $*
    echo
    echo "Test End: $*"
    echo
}

function testnoawt() {
    jrun 0 $* 2>&1 | tee -a java-run.log
}

function testawt() {
    MOSX_MT=0
    jrun 1 $* 2>&1 | tee -a java-run.log
}

function testawtmt() {
    jrun 1 $* 2>&1 | tee -a java-run.log
}

#
# newt (testnoawt and testawt)
#
#testnoawt com.jogamp.nativewindow.NativeWindowVersion $*
#testnoawt com.jogamp.opengl.JoglVersion $*
#testnoawt com.jogamp.newt.NewtVersion $*
#testnoawt com.jogamp.newt.opengl.GLWindow $*
#testnoawt com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen01GLPBufferNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.offscreen.TestOffscreen02BitmapNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLDebug00NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLDebug01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGLProfile01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextListNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES1NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextVBOES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestRemoteWindow01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestRemoteGLWindows01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.gl2.newt.TestGearsNEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es1.newt.TestGearsES1NEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestGearsES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es1.newt.TestRedSquareES1NEWT $*
testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestRedSquareES2NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestWindows01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestWindowClosingProtocol02NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestGLWindows01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestGLWindows02NEWTAnimated $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestDisplayLifecycle01NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestDisplayLifecycle02NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting02NEWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestScreenMode00NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestScreenMode00bNEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestScreenMode01NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestScreenMode01bNEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestScreenMode02NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.ManualScreenMode03NEWT
#testnoawt com.jogamp.opengl.test.junit.newt.TestWindowClosingProtocol02NEWT $*
#testnoawt -Djava.awt.headless=true com.jogamp.opengl.test.junit.newt.TestGLWindows01NEWT
#testnoawt com.jogamp.opengl.test.junit.jogl.swt.TestSWT01GLn $*
#testnoawt com.jogamp.opengl.test.junit.jogl.swt.TestSWT02GLn $*


#
# awt (testawt)
#
#testawt jogamp.newt.awt.opengl.VersionApplet $*
#testawt javax.media.opengl.awt.GLCanvas $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT01GLn $*
#testawt com.jogamp.opengl.test.junit.jogl.acore.TestSharedContextListAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestSwingAWT01GLn
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT03GLCanvasRecreate01 $*
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestAWT02WindowClosing
#testawt com.jogamp.opengl.test.junit.jogl.awt.text.TestAWTTextRendererUseVertexArrayBug464
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.awt.TestGearsAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.awt.TestGearsGLJPanelAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.texture.TestTexture01AWT
#testawt com.jogamp.opengl.test.junit.jogl.caps.TestMultisampleAWT
#testawt com.jogamp.opengl.test.junit.jogl.awt.TestBug461OffscreenSupersamplingSwingAWT
#testawt com.jogamp.opengl.test.junit.jogl.texture.TestGrayTextureFromFileAWTBug417
#testawt com.jogamp.opengl.test.junit.jogl.swt.TestSWTAWT01GLn $*
#testawt com.jogamp.opengl.test.junit.jogl.glu.TestBug463ScaleImageMemoryAWT $*

#
# newt.awt (testawt)
#
#testawt com.jogamp.opengl.test.junit.jogl.newt.TestSwingAWTRobotUsageBeforeJOGLInitBug411
#testawt com.jogamp.opengl.test.junit.jogl.demos.gl2.newt.TestGearsNewtAWTWrapper
#testawt com.jogamp.opengl.test.junit.newt.TestEventSourceNotAWTBug
#testawt com.jogamp.opengl.test.junit.newt.TestFocus01SwingAWTRobot $*
#testawt com.jogamp.opengl.test.junit.newt.TestFocus02SwingAWTRobot
#testawt com.jogamp.opengl.test.junit.newt.TestListenerCom01AWT
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01aAWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01bAWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01cAWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting01cSwingAWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting02AWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting03AWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting03AWT $*
#testawt com.jogamp.opengl.test.junit.newt.parenting.TestParenting03bAWT $*
#testawt com.jogamp.opengl.test.junit.newt.TestCloseNewtAWT
#testawt com.jogamp.opengl.test.junit.jogl.caps.TestMultisampleAWT $*
#testawt com.jogamp.opengl.test.junit.jogl.caps.TestMultisampleNEWT $*
#testawt com.jogamp.opengl.test.junit.jogl.glsl.TestShaderCompilationBug459AWT

#testawt com.jogamp.opengl.test.junit.newt.TestWindowClosingProtocol01AWT $*
#testnoawt com.jogamp.opengl.test.junit.newt.TestWindowClosingProtocol02NEWT $*
#testawt com.jogamp.opengl.test.junit.newt.TestWindowClosingProtocol03NewtAWT $*

#testawt $*

#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestTransformFeedbackVaryingsBug407NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLSimple01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestGLSLShaderState02NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestRulerNEWT01 $*
#testnoawt com.jogamp.opengl.test.junit.jogl.glsl.TestFBOMRTNEWT01 $*

#testnoawt com.jogamp.opengl.test.junit.graph.TestRegionRendererNEWT01 $*
#testnoawt com.jogamp.opengl.test.junit.graph.TestTextRendererNEWT01 $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.ui.UINewtDemo01 $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo01 $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.GPUTextNewtDemo02 $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.GPURegionNewtDemo01 $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.GPURegionNewtDemo02 $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.GPUUISceneNewtDemo01 $*
#testnoawt com.jogamp.opengl.test.junit.graph.demos.GPUUISceneNewtDemo02 $*

#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestGPUMemSec01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.acore.TestMapBuffer01NEWT $*
#testnoawt com.jogamp.opengl.test.junit.jogl.demos.es2.newt.TestElektronenMultipliziererNEWT $*

#
# regressions
#

$spath/count-edt-start.sh java-run.log

