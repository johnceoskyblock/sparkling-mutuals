package net.johnceo.sparklingmutuals.config

// UI metadata belongs to this mod; no settings-library runtime or persistence migration is required.
@Target(AnnotationTarget.FIELD) @Retention(AnnotationRetention.RUNTIME)
annotation class Category(val name: String, val desc: String = "")
@Target(AnnotationTarget.FIELD) @Retention(AnnotationRetention.RUNTIME)
annotation class ConfigOption(val name: String, val desc: String = "")
@Target(AnnotationTarget.FIELD) @Retention(AnnotationRetention.RUNTIME) annotation class Accordion
@Target(AnnotationTarget.FIELD) @Retention(AnnotationRetention.RUNTIME) annotation class StandaloneDescription
@Target(AnnotationTarget.FIELD) @Retention(AnnotationRetention.RUNTIME) annotation class ConfigEditorBoolean
@Target(AnnotationTarget.FIELD) @Retention(AnnotationRetention.RUNTIME) annotation class ConfigEditorText
@Target(AnnotationTarget.FIELD) @Retention(AnnotationRetention.RUNTIME) annotation class ConfigEditorColour
@Target(AnnotationTarget.FIELD) @Retention(AnnotationRetention.RUNTIME)
annotation class ConfigEditorSlider(val minValue: Float, val maxValue: Float, val minStep: Float)
@Target(AnnotationTarget.FIELD) @Retention(AnnotationRetention.RUNTIME)
annotation class ConfigEditorDropdown(val values: Array<String>)
@Target(AnnotationTarget.FIELD) @Retention(AnnotationRetention.RUNTIME)
annotation class ConfigEditorButton(val runnableId: Int = -1, val buttonText: String = "Open")
