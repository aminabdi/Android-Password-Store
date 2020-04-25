/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.zeapo.pwdstore.repository

import android.content.SharedPreferences
import com.zeapo.pwdstore.utils.PasswordItem

enum class PasswordSortOrder(val comparator: Comparator<PasswordItem>) {

    FOLDER_FIRST(Comparator { p1: PasswordItem, p2: PasswordItem ->
        (p1.type + p1.name)
                .compareTo(p2.type + p2.name, ignoreCase = true)
    }),

    INDEPENDENT(Comparator { p1: PasswordItem, p2: PasswordItem ->
        p1.name.compareTo(p2.name, ignoreCase = true)
    }),

    FILE_FIRST(Comparator { p1: PasswordItem, p2: PasswordItem ->
        (p2.type + p1.name).compareTo(p1.type + p2.name, ignoreCase = true)
    });

    companion object {
        fun getSortOrder(settings: SharedPreferences): PasswordSortOrder {
            return valueOf(settings.getString("sort_order", null) ?: FOLDER_FIRST.name)
        }
    }
}
