package(default_visibility = ['//visibility:public'])

java_library(
  name = 'jgit-archive',
  srcs = glob(
    ['src/**'],
    exclude = ['src/org/eclipse/jgit/archive/FormatActivator.java'],
  ),
  resources = glob(['resources/**']),
  resource_strip_prefix = 'org.eclipse.jgit.archive/resources',
  deps = [ # We want these deps to be provided_deps
    '//org.eclipse.jgit:jgit',
    '@commons_compress//jar',
  ],
)
