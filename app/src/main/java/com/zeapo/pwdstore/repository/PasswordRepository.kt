/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.zeapo.pwdstore.repository

import android.content.Context
import com.zeapo.pwdstore.utils.PasswordItem
import java.io.File
import org.eclipse.jgit.lib.Repository

interface PasswordRepository {

    /**
     * Initialize the repository with the initial state required for it to function.
     */
    fun initialize(context: Context)

    /**
     * Create a repository from scratch
     */
    fun createRepository(context: Context, dir: File)

    /**
     * Get an instance of a [Repository] or null if there is none.
     */
    fun getRepository(): Repository?

    /**
     * Get a list of files in the repository.
     */
    fun getFilesList(path: File): ArrayList<File>

    /**
     * Get a list of passwords in a given directory sorted by the given [PasswordSortOrder].
     */
    fun listPasswords(path: File, rootDir: File, sortOrder: PasswordSortOrder): ArrayList<PasswordItem>

    /**
     * Returns a boolean indicating if the initialization sequence for this repository has been
     * completed or not.
     */
    fun isInitialized(): Boolean

    /**
     * Get a [File] pointing to the root directory of the repository.
     */
    fun getRepositoryDirectory(context: Context): File
}
