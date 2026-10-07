package com.sliit.ecommerce.util;

import java.util.Collection;

public final class IdGenerator {
    private IdGenerator() {}

    public static String nextId(String prefix, Collection<String> existingIds) {
        int max = 0;
        if (existingIds != null) {
            for (String id : existingIds) {
                if (id == null || !id.startsWith(prefix)) continue;
                String number = id.substring(prefix.length());
                try {
                    max = Math.max(max, Integer.parseInt(number));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return prefix + String.format("%03d", max + 1);
    }


}
