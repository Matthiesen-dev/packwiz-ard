/**
 * Code adapted from "Packwiz Modpack Loader" by EvieTheOwl
 * Original Repository: https://git.gay/EvieTheOwl/packwiz-modpack-loader
 * Licensed under the MIT License.
 * Copyright (c) 2023 The Tiny Taters
 * Modified Copyright (c) 2026 Adam Matthiesen (Matthiesen-dev)
 */
package dev.matthiesen.packwiz_ard.common.shared.exceptions;

public class PackUrlException extends Exception {
  private static final String EXCEPTION_START = "Failed to read the configured modpack source. ";

  public PackUrlException(String message) {
    super(EXCEPTION_START + message);
  }
}
