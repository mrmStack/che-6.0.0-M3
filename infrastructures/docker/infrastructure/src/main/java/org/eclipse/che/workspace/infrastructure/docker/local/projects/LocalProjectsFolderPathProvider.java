/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.docker.local.projects;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.util.SystemInfo;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.workspace.infrastructure.docker.WindowsHostUtils;

/**
 * Provides path to workspace projects folder on host.
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class LocalProjectsFolderPathProvider {

  public static final String ALLOW_FOLDERS_CREATION_ENV_VARIABLE =
      "CHE_WORKSPACE_STORAGE_CREATE_FOLDERS";
  public static final String WORKSPACE_STORAGE_PATH_ENV_VARIABLE = "CHE_WORKSPACE_STORAGE";

  private final WorkspaceDao workspaceDao;
  private final boolean isWindows;

  /**
   * Value provide path to directory on host machine where will by all created and mount to the
   * created workspaces folder that become root of workspace inside machine. Inside machine it will
   * point to the directory described by {@literal che.workspace.projects.storage}.
   *
   * <p>For example: if you set {@literal che.workspaces.storage} to the /home/user/che/workspaces
   * after creating new workspace will be created new folder
   * /home/user/che/workspaces/{workspaceName} and it will be mount to the dev-machine to {@literal
   * che.workspace.projects.storage}
   */
  private String workspacesMountPoint;
  /**
   * this value provide path to projects on local host if this value will be set all workspace will
   * manage same projects from your host
   */
  @Inject(optional = true)
  @Named("host.projects.root")
  private String hostProjectsFolder;

  /**
   * If environment variable with name {@link #ALLOW_FOLDERS_CREATION_ENV_VARIABLE} is equal
   * (ignoring case) to {@literal false} then this field is set to false. Otherwise it is set to
   * true. It is also possible to overwrite with named constant.
   */
  @Inject(optional = true)
  @Named("che.workspace.storage.create_folders")
  private boolean createFolders = true;

  @Inject(optional = true)
  @Named("che.user.workspaces.storage")
  private String oldWorkspacesMountPoint;

  @Inject
  public LocalProjectsFolderPathProvider(
      @Named("che.workspace.storage") String workspacesMountPoint, WorkspaceDao workspaceDao)
      throws IOException {
    this.workspacesMountPoint = workspacesMountPoint;
    this.workspaceDao = workspaceDao;
    this.isWindows = SystemInfo.isWindows();
  }

  @VisibleForTesting
  protected LocalProjectsFolderPathProvider(
      String workspacesMountPoint,
      String oldWorkspacesMountPoint,
      String projectsFolder,
      boolean createFolders,
      WorkspaceDao workspaceDao,
      boolean isWindows)
      throws IOException {
    this.workspaceDao = workspaceDao;
    this.workspacesMountPoint = workspacesMountPoint;
    this.hostProjectsFolder = projectsFolder;
    this.createFolders = createFolders;
    this.oldWorkspacesMountPoint = oldWorkspacesMountPoint;
    this.isWindows = isWindows;
  }

  public String getPath(String workspaceId) throws IOException {
    if (!isWindows && hostProjectsFolder != null) {
      return hostProjectsFolder;
    }
    try {
      Workspace workspace = workspaceDao.get(workspaceId);
      String wsName = workspace.getConfig().getName();
      return doGetPathByName(wsName);
    } catch (NotFoundException | ServerException e) {
      throw new IOException(e.getLocalizedMessage());
    }
  }

  public String getPathByName(String workspaceName) throws IOException {
    if (!isWindows && hostProjectsFolder != null) {
      return hostProjectsFolder;
    }
    return doGetPathByName(workspaceName);
  }

  private String doGetPathByName(String workspaceName) throws IOException {
    final String workspaceFolderPath =
        Paths.get(workspacesMountPoint).resolve(workspaceName).toString();
    ensureExist(workspaceFolderPath, null);
    return workspaceFolderPath;
  }

  @VisibleForTesting
  @PostConstruct
  void init() throws IOException {
    // check folders creation flag from environment variable
    String allowFoldersCreationEnvVar = System.getenv(ALLOW_FOLDERS_CREATION_ENV_VARIABLE);
    if ("false".equalsIgnoreCase(allowFoldersCreationEnvVar)) {
      createFolders = false;
    }

    // Priority of workspace storage path sources:
    // If Che is running on Windows
    //     che-home-location/vfs
    // Otherwise
    //     If environment variable for storage location is set
    //         use value of that variable
    //     Otherwise
    //         If old property of workspace storage is set
    //             use value of that property for backward compatibility
    //         Otherwise
    //             use up-to-date property
    // find root directory for projects in workspaces
    if (isWindows) {
      final Path vfs = WindowsHostUtils.getCheHome().resolve("vfs");
      workspacesMountPoint = vfs.toString();
    } else {
      String workspaceStorageFromEnv = System.getenv(WORKSPACE_STORAGE_PATH_ENV_VARIABLE);
      if (workspaceStorageFromEnv != null) {
        workspacesMountPoint = workspaceStorageFromEnv;
      } else if (oldWorkspacesMountPoint != null) {
        workspacesMountPoint = oldWorkspacesMountPoint;
      }
    }

    // create directories if needed
    if (hostProjectsFolder == null) {
      ensureExist(
          workspacesMountPoint,
          oldWorkspacesMountPoint == null
              ? "che.workspace.storage"
              : "che.user.workspaces.storage");
    } else {
      ensureExist(hostProjectsFolder, "host.projects.root");
    }
  }

  private void ensureExist(String path, String prop) throws IOException {

    if (createFolders) {
      Path folder = Paths.get(path);
      if (Files.exists(folder)) {
        if (!Files.isDirectory(folder)) {
          if (prop != null) {
            throw new IOException(
                format(
                    "Workspace folder '%s' is not directory. Check %s configuration property",
                    path, prop));
          } else {
            throw new IOException(format("Workspace folder '%s' is not directory", path));
          }
        }
      } else {
        try {
          // TODO we should not create folders in this provider
          Files.createDirectories(folder);
        } catch (AccessDeniedException e) {
          throw new IOException(
              format(
                  "Workspace folder '%s' creation failed. Please check permissions of this folder. Cause: %s",
                  path, e.getLocalizedMessage()),
              e);
        } catch (IOException e) {
          throw new IOException(
              format(
                  "Workspace folder '%s' creation failed. Cause: %s",
                  path, e.getLocalizedMessage()),
              e);
        }
      }
    }
  }
}
