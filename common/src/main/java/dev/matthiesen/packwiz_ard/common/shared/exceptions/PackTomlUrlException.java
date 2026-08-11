package dev.matthiesen.packwiz_ard.common.shared.exceptions;

public class PackTomlUrlException extends Exception {
  private static final String EXCEPTION_START = "Failed to read the Packwiz pack.toml file. ";

  public PackTomlUrlException(String message) {
    super(EXCEPTION_START + message);
  }
}
