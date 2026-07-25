package com.meitu.generator.ui.projects;

import com.meitu.generator.data.local.dao.TaskDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class ProjectViewModel_Factory implements Factory<ProjectViewModel> {
  private final Provider<TaskDao> taskDaoProvider;

  public ProjectViewModel_Factory(Provider<TaskDao> taskDaoProvider) {
    this.taskDaoProvider = taskDaoProvider;
  }

  @Override
  public ProjectViewModel get() {
    return newInstance(taskDaoProvider.get());
  }

  public static ProjectViewModel_Factory create(Provider<TaskDao> taskDaoProvider) {
    return new ProjectViewModel_Factory(taskDaoProvider);
  }

  public static ProjectViewModel newInstance(TaskDao taskDao) {
    return new ProjectViewModel(taskDao);
  }
}
