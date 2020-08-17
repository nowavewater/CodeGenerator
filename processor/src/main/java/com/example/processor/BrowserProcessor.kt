package com.example.processor

import com.example.annotation.BindFragment
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic

@SupportedSourceVersion(SourceVersion.RELEASE_8)
class BrowserProcessor: AbstractProcessor() {
    private val annotation = BindFragment::class.java

    private lateinit var elementUtils: Elements
    private lateinit var messager: Messager
    private lateinit var options: Map<String, String>

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        elementUtils = processingEnv!!.elementUtils
        messager = processingEnv.messager
        options = processingEnv.options

    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(annotation).forEach { annotatedElement ->
            if (annotatedElement.kind == ElementKind.CLASS) {
                val pack = elementUtils.getPackageOf(annotatedElement).toString()
                val annotatedClassName = annotatedElement.simpleName.toString()
                startClassGeneration(pack, annotatedClassName)
            } else {
                messager.printMessage(Diagnostic.Kind.ERROR, "Cannot annotate anything but a class")
            }
        }
        return false
    }

    override fun getSupportedAnnotationTypes(): Set<String> = setOf(annotation.canonicalName)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    private fun startClassGeneration(pack: String, annotatedClassName: String) {
        val fileName = "${annotatedClassName}Browser"
        val contextPackager = ClassName("android.support.v7.app", "AppCompatActivity")
        val callerMethod = FunSpec.builder("start$annotatedClassName")
            .addParameter("activity", contextPackager)
            .addParameter("resourceIdToBeReplaced", Int::class)
            .returns(Int::class)
            .addCode(
                """
                 return activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(resourceIdToBeReplaced,$annotatedClassName.newInstance())
                .commit()
            """.trimIndent()
            ).build()
        val generatedClass = TypeSpec.objectBuilder(fileName).addFunction(callerMethod).build()
        val generatedFile = FileSpec.builder(pack, fileName).addType(generatedClass).build()
        val kaptKotlinGeneratedDir = options[KOTLIN_DIRECTORY_NAME]
        generatedFile.writeTo(File(kaptKotlinGeneratedDir, "$fileName.kt"))
    }

    companion object {
        const val KOTLIN_DIRECTORY_NAME = "kapt.kotlin.generated"
    }
}