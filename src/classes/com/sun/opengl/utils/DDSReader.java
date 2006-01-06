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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.utils;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/** A reader for DirectDraw Surface (.dds) files, which are used to
    describe textures. These files can contain multiple mipmap levels
    in one file. This reader is currently minimal and does not support
    all of the possible file formats. */

public class DDSReader {

  /** Simple class describing images and data; does not encapsulate
      image format information. User is responsible for transmitting
      that information in another way. */

  public static class ImageInfo {
    private ByteBuffer data;
    private int width;
    private int height;
    private boolean isCompressed;
    private int compressionFormat;

    public ImageInfo(ByteBuffer data, int width, int height, boolean compressed, int compressionFormat) {
      this.data = data; this.width = width; this.height = height;
      this.isCompressed = compressed; this.compressionFormat = compressionFormat;
    }
    public int        getWidth()  { return width;  }
    public int        getHeight() { return height; }
    public ByteBuffer getData()   { return data;   }
    public boolean    isCompressed() { return isCompressed; }
    public int        getCompressionFormat() {
      if (!isCompressed())
        throw new RuntimeException("Should not call unless compressed");
      return compressionFormat;
    }
  }

  private FileInputStream fis;
  private FileChannel     chan;
  private ByteBuffer buf;
  private Header header;

  //
  // Selected bits in header flags
  //

  public static final int DDSD_CAPS            = 0x00000001; // Capacities are valid
  public static final int DDSD_HEIGHT          = 0x00000002; // Height is valid
  public static final int DDSD_WIDTH           = 0x00000004; // Width is valid
  public static final int DDSD_PITCH           = 0x00000008; // Pitch is valid
  public static final int DDSD_BACKBUFFERCOUNT = 0x00000020; // Back buffer count is valid
  public static final int DDSD_ZBUFFERBITDEPTH = 0x00000040; // Z-buffer bit depth is valid (shouldn't be used in DDSURFACEDESC2)
  public static final int DDSD_ALPHABITDEPTH   = 0x00000080; // Alpha bit depth is valid
  public static final int DDSD_LPSURFACE       = 0x00000800; // lpSurface is valid
  public static final int DDSD_PIXELFORMAT     = 0x00001000; // ddpfPixelFormat is valid
  public static final int DDSD_MIPMAPCOUNT     = 0x00020000; // Mip map count is valid
  public static final int DDSD_LINEARSIZE      = 0x00080000; // dwLinearSize is valid
  public static final int DDSD_DEPTH           = 0x00800000; // dwDepth is valid

  public static final int DDPF_ALPHAPIXELS     = 0x00000001; // Alpha channel is present
  public static final int DDPF_ALPHA           = 0x00000002; // Only contains alpha information
  public static final int DDPF_FOURCC          = 0x00000004; // FourCC code is valid
  public static final int DDPF_PALETTEINDEXED4 = 0x00000008; // Surface is 4-bit color indexed
  public static final int DDPF_PALETTEINDEXEDTO8 = 0x00000010; // Surface is indexed into a palette which stores indices
                                                               // into the destination surface's 8-bit palette
  public static final int DDPF_PALETTEINDEXED8 = 0x00000020; // Surface is 8-bit color indexed
  public static final int DDPF_RGB             = 0x00000040; // RGB data is present
  public static final int DDPF_COMPRESSED      = 0x00000080; // Surface will accept pixel data in the format specified
                                                             // and compress it during the write
  public static final int DDPF_RGBTOYUV        = 0x00000100; // Surface will accept RGB data and translate it during
                                                             // the write to YUV data. The format of the data to be written
                                                             // will be contained in the pixel format structure. The DDPF_RGB
                                                             // flag will be set.
  public static final int DDPF_YUV             = 0x00000200; // Pixel format is YUV - YUV data in pixel format struct is valid
  public static final int DDPF_ZBUFFER         = 0x00000400; // Pixel format is a z buffer only surface
  public static final int DDPF_PALETTEINDEXED1 = 0x00000800; // Surface is 1-bit color indexed
  public static final int DDPF_PALETTEINDEXED2 = 0x00001000; // Surface is 2-bit color indexed
  public static final int DDPF_ZPIXELS         = 0x00002000; // Surface contains Z information in the pixels

  // Selected bits in DDS capabilities flags
  public static final int DDSCAPS_TEXTURE      = 0x00001000; // Can be used as a texture
  public static final int DDSCAPS_MIPMAP       = 0x00400000; // Is one level of a mip-map

  // Known pixel formats
  public static final int D3DFMT_UNKNOWN   =  0;
  public static final int D3DFMT_R8G8B8    =  20;
  public static final int D3DFMT_A8R8G8B8  =  21;
  public static final int D3DFMT_X8R8G8B8  =  22;
  // The following are also valid FourCC codes
  public static final int D3DFMT_DXT1      =  0x31545844;
  public static final int D3DFMT_DXT2      =  0x32545844;
  public static final int D3DFMT_DXT3      =  0x33545844;
  public static final int D3DFMT_DXT4      =  0x34545844;
  public static final int D3DFMT_DXT5      =  0x35545844;

  public void loadFile(String filename) throws IOException {
    loadFile(new File(filename));
  }

  public void loadFile(File file) throws IOException {
    fis = new FileInputStream(file);
    chan = fis.getChannel();
    buf = chan.map(FileChannel.MapMode.READ_ONLY,
                   0, (int) file.length());
    buf.order(ByteOrder.LITTLE_ENDIAN);
    header = new Header();
    header.read(buf);
    fixupHeader();
  }

  public void close() {
    try {
      chan.close();
      fis.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /** Test for presence/absence of surface description flags (DDSD_*) */
  public boolean isSurfaceDescFlagSet(int flag) {
    return ((header.flags & flag) != 0);
  }

  /** Test for presence/absence of pixel format flags (DDPF_*) */
  public boolean isPixelFormatFlagSet(int flag) {
    return ((header.pfFlags & flag) != 0);
  }

  /** Gets the pixel format of this texture (D3DFMT_*) based on some
      heuristics. Returns D3DFMT_UNKNOWN if could not recognize the
      pixel format. */
  public int getPixelFormat() {
    if (isCompressed()) {
      return getCompressionFormat();
    } else if (isPixelFormatFlagSet(DDPF_RGB)) {
      if (isPixelFormatFlagSet(DDPF_ALPHAPIXELS)) {
        if (getDepth() == 32 &&
            header.pfRBitMask == 0x00FF0000 &&
            header.pfGBitMask == 0x0000FF00 &&
            header.pfBBitMask == 0x000000FF &&
            header.pfABitMask == 0xFF000000) {
          return D3DFMT_A8R8G8B8;
        }
      } else {
        if (getDepth() == 24 &&
            header.pfRBitMask == 0x00FF0000 &&
            header.pfGBitMask == 0x0000FF00 &&
            header.pfBBitMask == 0x000000FF) {
          return D3DFMT_R8G8B8;
        } else if (getDepth() == 32 &&
                   header.pfRBitMask == 0x00FF0000 &&
                   header.pfGBitMask == 0x0000FF00 &&
                   header.pfBBitMask == 0x000000FF) {
          return D3DFMT_X8R8G8B8;
        }
      }
    }

    return D3DFMT_UNKNOWN;
  }

  /** Indicates whether this texture is compressed. */
  public boolean isCompressed() {
    return (isPixelFormatFlagSet(DDPF_FOURCC));
  }

  /** If this surface is compressed, returns the kind of compression
      used (DXT1..DXT5). */
  public int getCompressionFormat() {
    return header.pfFourCC;
  }

  /** Width of the texture (or the top-most mipmap if mipmaps are
      present) */
  public int getWidth() {
    return header.width;
  }

  /** Height of the texture (or the top-most mipmap if mipmaps are
      present) */
  public int getHeight() {
    return header.height;
  }

  /** Total number of bits per pixel. Only valid if DDPF_RGB is
      present. For A8R8G8B8, would be 32. */
  public int getDepth() {
    return header.pfRGBBitCount;
  }

  /** Number of mip maps in the texture */
  public int getNumMipMaps() {
    if (!isSurfaceDescFlagSet(DDSD_MIPMAPCOUNT)) {
      return 0;
    }
    return header.mipMapCountOrAux;
  }

  /** Gets the <i>i</i>th mipmap data (0..getNumMipMaps() - 1) */
  public ImageInfo getMipMap(int map) {
    if (getNumMipMaps() > 0 &&
        ((map < 0) || (map >= getNumMipMaps()))) {
      throw new RuntimeException("Illegal mipmap number " + map + " (0.." + (getNumMipMaps() - 1) + ")");
    }

    // Figure out how far to seek
    int seek = 4 + header.size;
    for (int i = 0; i < map; i++) {
      seek += mipMapSizeInBytes(i);
    }
    buf.limit(seek + mipMapSizeInBytes(map));
    buf.position(seek);
    ByteBuffer next = buf.slice();
    buf.position(0);
    buf.limit(buf.capacity());
    return new ImageInfo(next, mipMapWidth(map), mipMapHeight(map), isCompressed(), getCompressionFormat());
  }

  /** Returns an array of ImageInfos corresponding to all mipmap
      levels of this DDS file. */
  public ImageInfo[] getAllMipMaps() {
    int numLevels = getNumMipMaps();
    if (numLevels == 0) {
      numLevels = 1;
    }
    ImageInfo[] result = new ImageInfo[numLevels];
    for (int i = 0; i < numLevels; i++) {
      result[i] = getMipMap(i);
    }
    return result;
  }

  /** Converts e.g. DXT1 compression format constant (see {@link
      #getCompressionFormat}) into "DXT1". */
  public static String getCompressionFormatName(int compressionFormat) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < 4; i++) {
      char c = (char) (compressionFormat & 0xFF);
      buf.append(c);
      compressionFormat = compressionFormat >> 8;
    }
    return buf.toString();
  }

  public void debugPrint() {
    PrintStream tty = System.err;
    tty.println("Compressed texture: " + isCompressed());
    if (isCompressed()) {
      int fmt = getCompressionFormat();
      String name = getCompressionFormatName(fmt);
      tty.println("Compression format: 0x" + Integer.toHexString(fmt) + " (" + name + ")");
    }
    tty.println("Width: " + header.width + " Height: " + header.height);
    tty.println("header.pitchOrLinearSize: " + header.pitchOrLinearSize);
    tty.println("header.pfRBitMask: 0x" + Integer.toHexString(header.pfRBitMask));
    tty.println("header.pfGBitMask: 0x" + Integer.toHexString(header.pfGBitMask));
    tty.println("header.pfBBitMask: 0x" + Integer.toHexString(header.pfBBitMask));
    tty.println("SurfaceDesc flags:");
    boolean recognizedAny = false;
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_CAPS, "DDSD_CAPS");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_HEIGHT, "DDSD_HEIGHT");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_WIDTH, "DDSD_WIDTH");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_PITCH, "DDSD_PITCH");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_BACKBUFFERCOUNT, "DDSD_BACKBUFFERCOUNT");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_ZBUFFERBITDEPTH, "DDSD_ZBUFFERBITDEPTH");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_ALPHABITDEPTH, "DDSD_ALPHABITDEPTH");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_LPSURFACE, "DDSD_LPSURFACE");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_PIXELFORMAT, "DDSD_PIXELFORMAT");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_MIPMAPCOUNT, "DDSD_MIPMAPCOUNT");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_LINEARSIZE, "DDSD_LINEARSIZE");
    recognizedAny |= printIfRecognized(tty, header.flags, DDSD_DEPTH, "DDSD_DEPTH");
    if (!recognizedAny) {
      tty.println("(none)");
    }
    tty.println("Raw SurfaceDesc flags: 0x" + Integer.toHexString(header.flags));
    tty.println("Pixel format flags:");
    recognizedAny = false;
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_ALPHAPIXELS, "DDPF_ALPHAPIXELS");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_ALPHA, "DDPF_ALPHA");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_FOURCC, "DDPF_FOURCC");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_PALETTEINDEXED4, "DDPF_PALETTEINDEXED4");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_PALETTEINDEXEDTO8, "DDPF_PALETTEINDEXEDTO8");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_PALETTEINDEXED8, "DDPF_PALETTEINDEXED8");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_RGB, "DDPF_RGB");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_COMPRESSED, "DDPF_COMPRESSED");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_RGBTOYUV, "DDPF_RGBTOYUV");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_YUV, "DDPF_YUV");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_ZBUFFER, "DDPF_ZBUFFER");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_PALETTEINDEXED1, "DDPF_PALETTEINDEXED1");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_PALETTEINDEXED2, "DDPF_PALETTEINDEXED2");
    recognizedAny |= printIfRecognized(tty, header.pfFlags, DDPF_ZPIXELS, "DDPF_ZPIXELS");
    if (!recognizedAny) {
      tty.println("(none)");
    }
    tty.println("Raw pixel format flags: 0x" + Integer.toHexString(header.pfFlags));
    tty.println("Depth: " + getDepth());
    tty.println("Number of mip maps: " + getNumMipMaps());
    int fmt = getPixelFormat();
    tty.print("Pixel format: ");
    switch (fmt) {
    case D3DFMT_R8G8B8:   tty.println("D3DFMT_R8G8B8"); break;
    case D3DFMT_A8R8G8B8: tty.println("D3DFMT_A8R8G8B8"); break;
    case D3DFMT_X8R8G8B8: tty.println("D3DFMT_X8R8G8B8"); break;
    case D3DFMT_DXT1:     tty.println("D3DFMT_DXT1"); break;
    case D3DFMT_DXT2:     tty.println("D3DFMT_DXT2"); break;
    case D3DFMT_DXT3:     tty.println("D3DFMT_DXT3"); break;
    case D3DFMT_DXT4:     tty.println("D3DFMT_DXT4"); break;
    case D3DFMT_DXT5:     tty.println("D3DFMT_DXT5"); break;
    case D3DFMT_UNKNOWN:  tty.println("D3DFMT_UNKNOWN"); break;
    default:              tty.println("(unknown pixel format " + fmt + ")"); break;
    }
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static final int MAGIC = 0x20534444;

  static class Header {
    int size;                 // size of the DDSURFACEDESC structure
    int flags;                // determines what fields are valid
    int height;               // height of surface to be created
    int width;                // width of input surface
    int pitchOrLinearSize;
    int backBufferCountOrDepth;
    int mipMapCountOrAux;     // number of mip-map levels requested (in this context)
    int alphaBitDepth;        // depth of alpha buffer requested
    int reserved1;            // reserved
    int surface;              // pointer to the associated surface memory
    // NOTE: following two entries are from DDCOLORKEY data structure
    // Are overlaid with color for empty cubemap faces (unused in this reader)
    int colorSpaceLowValue;
    int colorSpaceHighValue;
    int destBltColorSpaceLowValue;
    int destBltColorSpaceHighValue;
    int srcOverlayColorSpaceLowValue;
    int srcOverlayColorSpaceHighValue;
    int srcBltColorSpaceLowValue;
    int srcBltColorSpaceHighValue;
    // NOTE: following entries are from DDPIXELFORMAT data structure
    // Are overlaid with flexible vertex format description of vertex
    // buffers (unused in this reader)
    int pfSize;                 // size of DDPIXELFORMAT structure
    int pfFlags;                // pixel format flags
    int pfFourCC;               // (FOURCC code)
    // Following five entries have multiple interpretations, not just
    // RGBA (but that's all we support right now)
    int pfRGBBitCount;          // how many bits per pixel
    int pfRBitMask;             // mask for red bits
    int pfGBitMask;             // mask for green bits
    int pfBBitMask;             // mask for blue bits
    int pfABitMask;             // mask for alpha channel
    int ddsCaps1;               // Texture and mip-map flags
    int ddsCaps2;               // Advanced capabilities, not yet used 
    int ddsCapsReserved1;
    int ddsCapsReserved2;
    int textureStage;           // stage in multitexture cascade

    void read(ByteBuffer buf) throws IOException {
      int magic                     = buf.getInt();
      if (magic != MAGIC) {
        throw new IOException("Incorrect magic number 0x" +
                              Integer.toHexString(magic) +
                              " (expected " + MAGIC + ")");
      }

      size                          = buf.getInt();
      flags                         = buf.getInt();
      height                        = buf.getInt();
      width                         = buf.getInt();
      pitchOrLinearSize             = buf.getInt();
      backBufferCountOrDepth        = buf.getInt();
      mipMapCountOrAux              = buf.getInt();
      alphaBitDepth                 = buf.getInt();
      reserved1                     = buf.getInt();
      surface                       = buf.getInt();
      colorSpaceLowValue            = buf.getInt();
      colorSpaceHighValue           = buf.getInt();
      destBltColorSpaceLowValue     = buf.getInt();
      destBltColorSpaceHighValue    = buf.getInt();
      srcOverlayColorSpaceLowValue  = buf.getInt();
      srcOverlayColorSpaceHighValue = buf.getInt();
      srcBltColorSpaceLowValue      = buf.getInt();
      srcBltColorSpaceHighValue     = buf.getInt();
      pfSize                        = buf.getInt();
      pfFlags                       = buf.getInt();
      pfFourCC                      = buf.getInt();
      pfRGBBitCount                 = buf.getInt();
      pfRBitMask                    = buf.getInt();
      pfGBitMask                    = buf.getInt();
      pfBBitMask                    = buf.getInt();
      pfABitMask                    = buf.getInt();
      ddsCaps1                      = buf.getInt();
      ddsCaps2                      = buf.getInt();
      ddsCapsReserved1              = buf.getInt();
      ddsCapsReserved2              = buf.getInt();
      textureStage                  = buf.getInt();
    }
  }

  // Microsoft doesn't follow their own specifications and the
  // simplest conversion using the DxTex tool to e.g. a DXT3 texture
  // results in an illegal .dds file without either DDSD_PITCH or
  // DDSD_LINEARSIZE set in the header's flags. This code, adapted
  // from the DevIL library, fixes up the header in these situations.
  private void fixupHeader() {
    if (isCompressed() && !isSurfaceDescFlagSet(DDSD_LINEARSIZE)) {
      // Figure out how big the linear size should be
      int depth = header.backBufferCountOrDepth;
      if (depth == 0) {
        depth = 1;
      }

      int blockSize = ((getWidth() + 3)/4) * ((getHeight() + 3)/4) * ((depth + 3)/4);
      switch (getCompressionFormat()) {
        case D3DFMT_DXT1:  blockSize *=  8; break;
        default:           blockSize *= 16; break;
      }

      header.pitchOrLinearSize = blockSize;
      header.flags |= DDSD_LINEARSIZE;
    }
  }

  private int mipMapWidth(int map) {
    int width = getWidth();
    for (int i = 0; i < map; i++) {
      width >>= 1;
    }
    return width;
  }

  private int mipMapHeight(int map) {
    int height = getHeight();
    for (int i = 0; i < map; i++) {
      height >>= 1;
    }
    return height;
  }

  private int mipMapSizeInBytes(int map) {
    if (isCompressed()) {
      if (!isSurfaceDescFlagSet(DDSD_LINEARSIZE)) {
        throw new RuntimeException("Illegal compressed texture: DDSD_LINEARSIZE not specified in texture header");
      }
      int bytes = header.pitchOrLinearSize;
      for (int i = 0; i < map; i++) {
        bytes >>= 2;
      }
      return bytes;
    } else {
      int width  = mipMapWidth(map);
      int height = mipMapHeight(map);
      return width * height * (getDepth() / 8);
    }
  }

  private boolean printIfRecognized(PrintStream tty, int flags, int flag, String what) {
    if ((flags & flag) != 0) {
      tty.println(what);
      return true;
    }
    return false;
  }
}
