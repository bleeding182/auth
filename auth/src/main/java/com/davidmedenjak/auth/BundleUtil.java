package com.davidmedenjak.auth;

import android.os.Bundle;
import android.support.annotation.Nullable;

class BundleUtil {

    /**
     * Print a bundle as Json-ish style string.
     *
     * @param bundle the bundle
     * @return a json-ish string of all keys/values
     */
    public static String toString(@Nullable Bundle bundle) {
        if (bundle == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (!first) {
                builder.append(", ");
            }
            builder.append("\"").append(key).append("\":");

            if (value instanceof Bundle) {
                builder.append(toString((Bundle) value));
            } else {
                builder.append("\"").append(value).append("\"");
            }
            first = false;
        }
        return builder.append("}").toString();
    }
}
