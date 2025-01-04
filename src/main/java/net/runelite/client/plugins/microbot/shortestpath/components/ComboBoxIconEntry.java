package net.runelite.client.plugins.microbot.shortestpath.components;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.swing.*;

@AllArgsConstructor
@Getter
public class ComboBoxIconEntry
{
    private Icon icon;
    private String text;
    @Nullable
    private Object data;
}