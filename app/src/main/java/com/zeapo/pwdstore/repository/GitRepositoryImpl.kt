/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.zeapo.pwdstore.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.zeapo.pwdstore.utils.PasswordItem
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class GitRepository : PasswordRepository {
    private var repository: Repository? = null
    private var settings: SharedPreferences? = null

    override fun initialize(context: Context) {
        settings = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        val dir = File(getRepositoryDirectory(context), ".git")
        if (!dir.exists() && !dir.mkdir()) throw RuntimeException("Failed to create repository directory")
        val builder = FileRepositoryBuilder()
        repository = try {
            builder.setGitDir(dir)
                    .readEnvironment()
                    .build()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun createRepository(context: Context, dir: File) {
        dir.deleteRecursively()
        Git.init().setDirectory(dir).call()
        initialize(context)
    }

    override fun getRepository(): Repository? = repository

    override fun getFilesList(path: File): ArrayList<File> {
        if (!path.exists()) return arrayListOf()
        return path.listFiles { file -> file.isDirectory || file.extension == ".gpg" }
                ?.toCollection(ArrayList()) ?: arrayListOf()
    }

    override fun listPasswords(path: File, rootDir: File, sortOrder: PasswordSortOrder): ArrayList<PasswordItem> {
        var passList = getFilesList(path)
        val passwordList = ArrayList<PasswordItem>()
        val showHiddenDirs = settings?.getBoolean("show_hidden_folders", false) ?: false

        if (passList.size == 0) return passwordList
        passList = ArrayList(if (showHiddenDirs) {
            passList.filter { !(it.isFile && it.isHidden) }
        } else {
            passList.filter { !it.isHidden }
        })
        passList.forEach { file ->
            passwordList.add(if (file.isFile) {
                PasswordItem.newPassword(file.name, file, rootDir)
            } else {
                PasswordItem.newCategory(file.name, file, rootDir)
            })
        }
        passwordList.sortWith(sortOrder.comparator)
        return passwordList
    }

    override fun isInitialized(): Boolean {
        val repo = repository ?: return false
        return repo.config.getSubsections("remote").isNotEmpty() &&
                repo.objectDatabase.exists() &&
                repo.allRefs.isNotEmpty()
    }

    override fun getRepositoryDirectory(context: Context): File {
        return if (settings?.getBoolean("git_external", false) == true) {
            val externalRepo = settings?.getString("git_external_repo", null)
            if (externalRepo != null)
                File(externalRepo)
            else
                File(context.filesDir, "store")
        } else {
            File(context.filesDir, "store")
        }
    }
}
