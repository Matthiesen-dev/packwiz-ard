package dev.matthiesen.packwiz_ard.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public final class PackWizardConfig {
    @SerializedName("pack_toml")
    public String pack_toml = "";

    @SerializedName("minimum_permission_level")
    public int minimum_permission_level = 4;

    @SuppressWarnings("unused")
    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
}
