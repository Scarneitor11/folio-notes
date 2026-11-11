# Compose ships its own consumer rules; blanket-keeping androidx.compose.** defeats R8.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
