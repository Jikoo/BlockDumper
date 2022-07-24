package com.github.jikoo.blockdumper.render;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImageWrite;

/**
 * A utility class for saving raw window data to PNG format.
 *
 * @param data the raw screen data in RGBA format
 * @param width the width of the region in pixels
 * @param height the height of the region in pixels
 * @param location the file to which the resulting image will be written.
 */
public record WindowToPng(@NotNull ByteBuffer data, int width, int height, @NotNull File location) {

  public static @NotNull ByteBuffer readWindowPixels(
      int startX,
      int startY,
      int width,
      int height)
      throws OutOfMemoryError {

      ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
      GL11.glReadPixels(startX, startY, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

      return buffer;
  }

  public void saveStbi() {
    data().rewind();
    STBImageWrite.stbi_flip_vertically_on_write(true);
    int channelsPerPixel = 4; // RGBA
    STBImageWrite.stbi_write_png(location().getAbsolutePath(), width(), height(), channelsPerPixel, data(), width() * channelsPerPixel);
  }


  /**
   * Translate buffer content into a {@link BufferedImage} and write it to disk using
   * {@link ImageIO}.
   *
   * <p>ImageIO is not as fast as STBImageWriter for small images, but exceeds it for larger files.
   * <pre>
   * Save times:
   * 16x16
   *   STBIW:    345_000 ns
   *   ImgIO:  1_000_000 ns
   * 1024x1024:
   *   STBIW: 83_250_000 ns
   *   ImgIO: 43_000_000 ns
   * </pre>
   * In addition, STBIW's compression is way worse.
   * <pre>
   * Disk usage (average for all 1151 vanilla items):
   * 16x16:
   *   STBIW:    447 B
   *   ImgIO:    284 B
   * 1024x1024:
   *   STBIW: 58_506 B
   *   ImgIO: 46_278 B
   * </pre>
   */
  public void save() throws IOException {
    // Create directories first - ImageIO will fail to generate a stream with a very unhelpful
    // message if they do not exist.
    Files.createDirectories(location().getParentFile().toPath());

    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    // Writing data directly to buffer's backing array is way more efficient than trying to use
    // awt's image manipulation methods. Drawing each pixel via a rectangle is very slow.
    // TYPE_INT_ARGB is backed by a DataBufferInt. The write process for a DataBufferInt is about 4%
    // faster than using a DataBufferByte with TYPE_4BYTE_ABGR.
    int[] data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

    ByteBuffer buffer = data();
    buffer.rewind();

    // Pixels are inverted in buffer - the lowest row is first out. Start at lowest row.
    for (int row = height - 1; row >= 0; --row) {
      for (int column = 0; column < width; ++column) {
        int pixel = row * height + column;
        // Convert RGBA bytes -> ARGB int.
        // First mask each byte with 0xff to ensure 0-255 limit, then shift to correct place.
        data[pixel] =
            ((buffer.get() & 0xff) << 16) // red: 0x00ff0000 mask
                | ((buffer.get() & 0xff) << 8) // green: 0x0000ff00 mask
                | ((buffer.get() & 0xff)) // blue: 0x000000ff mask
                | ((buffer.get() & 0xff) << 24); // alpha: 0xff000000 mask
      }
    }

    // Write file.
    ImageIO.write(image, "png", location());
  }

}
