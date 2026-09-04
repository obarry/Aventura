package com.aventura.model.texture;

/**
 * ------------------------------------------------------------------------------
 * Thrown when a Texture cannot be loaded from a file -- either because the file
 * itself couldn't be read (IOException), or because it was read but isn't a
 * recognized image format (ImageIO.read() returns null in that case, with no
 * exception of its own).
 *
 * Unchecked (extends RuntimeException) so that existing callers of
 * Texture(String) constructors -- none of which currently declare or catch a
 * checked exception -- keep compiling without change.
 *
 * @author Olivier BARRY
 * @since 2026
 *
 */
public class TextureLoadException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TextureLoadException(String message) {
		super(message);
	}

	public TextureLoadException(String message, Throwable cause) {
		super(message, cause);
	}
}