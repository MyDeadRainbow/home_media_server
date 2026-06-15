package com.hms.acquisition.importmedia.magnetfinder;

import com.microsoft.playwright.ElementHandle;

public record ElementSearchScore(int score, ElementHandle element) {
}
