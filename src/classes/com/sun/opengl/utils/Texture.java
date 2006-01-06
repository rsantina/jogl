/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package com.sun.opengl.utils;

import java.awt.geom.*;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

/**
 * Represents an OpenGL texture object. Contains convenience routines
 * for enabling/disabling OpenGL texture state, binding this texture,
 * and computing texture coordinates for both the entire image as well
 * as a sub-image. 
 * 
 * <br> REMIND: document GL_TEXTURE_2D/GL_TEXTURE_RECTANGLE_ARB issues...
 * <br> REMIND: translucent images will have premultiplied comps by default...
 *
 * @author Chris Campbell
 * @author Kenneth Russell
 */
public class Texture {
  /** The GL target type. */
  private int target;
  /** The GL texture ID. */
  private int texID;
  /** The width of the texture. */
  private int texWidth;
  /** The height of the texture. */
  private int texHeight;
  /** The width of the image. */
  private int imgWidth;
  /** The height of the image. */
  private int imgHeight;
  /** Indicates whether the TextureData requires a vertical flip of
      the texture coords. */
  private boolean mustFlipVertically;

  /** The texture coordinate corresponding to the lower left corner
      of the texture when properly oriented. */
  private Point2D lowerLeftTexCoord = new Point2D.Float();

  /** The texture coordinate corresponding to the upper right corner
      of the texture when properly oriented. */
  private Point2D upperRightTexCoord = new Point2D.Float();

  private static final boolean DEBUG = Debug.debug("Texture");

  // For now make Texture constructor package-private to limit the
  // number of public APIs we commit to
  Texture(TextureData data) throws GLException {
    GL gl = getCurrentGL();

    imgWidth = data.getWidth();
    imgHeight = data.getHeight();
    mustFlipVertically = data.getMustFlipVertically();

    if ((isPowerOfTwo(imgWidth) && isPowerOfTwo(imgHeight)) ||
        gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two")) {
      if (DEBUG) {
        if (isPowerOfTwo(imgWidth) && isPowerOfTwo(imgHeight)) {
          System.err.println("Power-of-two texture");
        } else {
          System.err.println("Using GL_ARB_texture_non_power_of_two");
        }
      }

      texWidth = imgWidth;
      texHeight = imgHeight;
      target = GL.GL_TEXTURE_2D;
    } else if (gl.isExtensionAvailable("GL_ARB_texture_rectangle")) {
      if (DEBUG) {
        System.err.println("Using GL_ARB_texture_rectangle");
      }

      texWidth = imgWidth;
      texHeight = imgHeight;
      target = GL.GL_TEXTURE_RECTANGLE_ARB;
    } else {
      if (DEBUG) {
        System.err.println("Expanding texture to power-of-two dimensions");
      }

      if (data.getBorder() != 0) {
        throw new RuntimeException("Scaling up a non-power-of-two texture which has a border won't work");
      }
      texWidth = nextPowerOfTwo(imgWidth);
      texHeight = nextPowerOfTwo(imgHeight);
      target = GL.GL_TEXTURE_2D;
    }

    // REMIND: let the user specify these, optionally
    int minFilter = GL.GL_LINEAR;
    int magFilter = GL.GL_LINEAR;
    int wrapMode = GL.GL_CLAMP_TO_EDGE;

    texID = createTextureID(gl); 
    setImageSize(imgWidth, imgHeight);
    gl.glBindTexture(target, texID);
    gl.glTexImage2D(target, 0, data.getInternalFormat(),
                    texWidth, texHeight, data.getBorder(),
                    data.getPixelFormat(), data.getPixelType(), null);

    // REMIND: figure out what to do for GL_TEXTURE_RECTANGLE_ARB
    if (target == GL.GL_TEXTURE_2D) {
      gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, minFilter);
      gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, magFilter);
      gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S, wrapMode);
      gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T, wrapMode);
    }

    updateSubImage(data, 0, 0);
  }

  /**
   * Enables this texture's target (e.g., GL_TEXTURE_2D) in the
   * current GL context's state.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void enable() throws GLException {
    getCurrentGL().glEnable(target); 
  }

  /**
   * Disables this texture's target (e.g., GL_TEXTURE_2D) in the
   * current GL context's state.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void disable() throws GLException {
    getCurrentGL().glDisable(target); 
  }

  /**
   * Binds this texture to the current GL context.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void bind() throws GLException {
    getCurrentGL().glBindTexture(target, texID); 
  }

  /**
   * Disposes the native resources used by this texture object.
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void dispose() throws GLException {
    getCurrentGL().glDeleteTextures(1, new int[] {texID}, 0);
  }

  /**
   * Returns the OpenGL "target" of this texture.
   *
   * @return the OpenGL target of this texture
   * @see javax.media.opengl.GL#GL_TEXTURE_2D
   * @see javax.media.opengl.GL#GL_TEXTURE_RECTANGLE_ARB
   */
  public int getTarget() {
    return target;
  }

  /**
   * Returns the width of the texture.  Note that the texture width will
   * be greater than or equal to the width of the image contained within.
   *
   * @return the width of the texture
   */
  public int getWidth() {
    return texWidth;
  }
    
  /**
   * Returns the height of the texture.  Note that the texture height will
   * be greater than or equal to the height of the image contained within.
   *
   * @return the height of the texture
   */
  public int getHeight() {
    return texHeight;
  }   
    
  /** 
   * Returns the width of the image contained within this texture.
   *
   * @return the width of the image
   */
  public int getImageWidth() {
    return imgWidth;
  }

  /**
   * Returns the height of the image contained within this texture.
   *
   * @return the height of the image
   */
  public int getImageHeight() {
    return imgHeight;
  }

  /**
   * Returns the texture coordinate corresponding to the lower-left
   * point of this texture when oriented properly. If the TextureData
   * indicated that the texture coordinates must be flipped
   * vertically, the returned Point will take that into account.
   *
   * @return the texture coordinate of the lower-left point of the
   * texture
   */
  public Point2D getImageLowerLeftTexCoord() {
    return (Point2D) lowerLeftTexCoord.clone();
  }

  /**
   * Returns the texture coordinate corresponding to the upper-right
   * point of this texture when oriented properly. If the TextureData
   * indicated that the texture coordinates must be flipped
   * vertically, the returned Point will take that into account.
   *
   * @return the texture coordinates of the upper-right point of the
   * texture
   */
  public Point2D getImageUpperRightTexCoord() {
    return (Point2D) upperRightTexCoord.clone();
  }


  /**
   * Returns the texture coordinate corresponding to the lower-left
   * point of the specified sub-image of this texture when oriented
   * properly. If the TextureData indicated that the texture
   * coordinates must be flipped vertically, the returned Point will
   * take that into account.
   *
   * @return the texture coordinate of the lower-left point of the
   * texture
   */
  public Point2D getSubImageLowerLeftTexCoord(int x1, int y1, int x2, int y2) {
    if (target == GL.GL_TEXTURE_RECTANGLE_ARB) {
      if (mustFlipVertically) {
        return new Point2D.Float(x1, texHeight - y1);
      } else {
        return new Point2D.Float(x1, y1);
      }
    } else {
      float tx = (float)x1 / (float)texWidth;
      float ty = (float)y1 / (float)texHeight;

      if (mustFlipVertically) {
        return new Point2D.Float(tx, 1.0f - ty);
      } else {
        return new Point2D.Float(tx, ty);
      }
    }
  }

  /**
   * Returns the texture coordinate corresponding to the upper-right
   * point of the specified sub-image of this texture when oriented
   * properly. If the TextureData indicated that the texture
   * coordinates must be flipped vertically, the returned Point will
   * take that into account.
   *
   * @return the texture coordinate of the upper-right point of the
   * texture
   */
  public Point2D getSubImageUpperRightTexCoord(int x1, int y1, int x2, int y2) {
    if (target == GL.GL_TEXTURE_RECTANGLE_ARB) {
      if (mustFlipVertically) {
        return new Point2D.Float(x2, texHeight - y2);
      } else {
        return new Point2D.Float(x2, y2);
      }
    } else {
      float tx = (float)x2 / (float)texWidth;
      float ty = (float)y2 / (float)texHeight;

      if (mustFlipVertically) {
        return new Point2D.Float(tx, 1.0f - ty);
      } else {
        return new Point2D.Float(tx, ty);
      }
    }
  }

  /**
   * Updates a subregion of the content area of this texture using the
   * data in the given image.
   *
   * @param data the image data to be uploaded to this texture
   * @param x the x offset (in pixels) relative to the lower-left corner
   * of this texture
   * @param y the y offset (in pixels) relative to the lower-left corner
   * of this texture
   *
   * @throws GLException if no OpenGL context was current or if any
   * OpenGL-related errors occurred
   */
  public void updateSubImage(TextureData data, int x, int y) throws GLException {
    GL gl = getCurrentGL();
    bind();
    if (data.isDataCompressed()) {
      // FIXME: should test availability of appropriate texture
      // compression extension here
      gl.glCompressedTexSubImage2D(target, data.getMipmapLevel(),
                                   x, y, data.getWidth(), data.getHeight(),
                                   data.getInternalFormat(), data.getBuffer().remaining(),
                                   data.getBuffer());
    } else {
      int[] align = new int[1];
      gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, align, 0); // save alignment
      gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, data.getAlignment());

      gl.glTexSubImage2D(target, data.getMipmapLevel(),
                         x, y, data.getWidth(), data.getHeight(),
                         data.getPixelFormat(), data.getPixelType(),
                         data.getBuffer());
      gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, align[0]); // restore align
    }
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  /**
   * Returns the current GL object. Throws GLException if no OpenGL
   * context was current.
   */
  private static GL getCurrentGL() throws GLException {
    GLContext context = GLContext.getCurrent();
    if (context == null) {
      throw new GLException("No OpenGL context current on current thread");
    }
    return context.getGL();
  }

  /**
   * Returns true if the given value is a power of two.
   *
   * @return true if the given value is a power of two, false otherwise
   */
  private static boolean isPowerOfTwo(int val) {
    return ((val & (val - 1)) == 0);
  }

  /**
   * Returns the nearest power of two that is larger than the given value.
   * If the given value is already a power of two, this method will simply
   * return that value.
   *
   * @param val the value
   * @return the next power of two
   */
  private static int nextPowerOfTwo(int val) {
    int ret = 1;
    while (ret < val) {
      ret <<= 1;
    }
    return ret;
  }

  /**
   * Updates the actual image dimensions; usually only called from
   * <code>updateImage</code>.
   */
  private void setImageSize(int width, int height) {
    imgWidth = width;
    imgHeight = height;
    if (target == GL.GL_TEXTURE_RECTANGLE_ARB) {
      if (mustFlipVertically) {
        lowerLeftTexCoord  = new Point2D.Float(0, imgHeight);
        upperRightTexCoord = new Point2D.Float(imgWidth, 0);
      } else {
        lowerLeftTexCoord  = new Point2D.Float(0, 0);
        upperRightTexCoord = new Point2D.Float(imgWidth, imgHeight);
      }
    } else {
      if (mustFlipVertically) {
        lowerLeftTexCoord  = new Point2D.Float(0, (float) imgHeight / (float) texHeight);
        upperRightTexCoord = new Point2D.Float((float) imgWidth / (float) texWidth, 0);
      } else {
        lowerLeftTexCoord  = new Point2D.Float(0, 0);
        upperRightTexCoord = new Point2D.Float((float) imgWidth / (float) texWidth,
                                               (float) imgHeight / (float) texHeight);
      }
    }
  }

  /**
   * Creates a new texture ID.
   *
   * @param gl the GL object associated with the current OpenGL context
   * @return a new texture ID
   */
  private static int createTextureID(GL gl) {
    int[] tmp = new int[1];
    gl.glGenTextures(1, tmp, 0);
    return tmp[0];
  }
}
