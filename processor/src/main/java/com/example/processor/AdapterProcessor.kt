package com.example.processor

import com.example.annotation.AdapterModel
import com.example.annotation.ViewHolderBinding
import com.example.processor.codegen.AdapterCodeBuilder
import com.example.processor.model.ModelData
import com.example.processor.model.ViewHolderBindingData
import com.squareup.kotlinpoet.FileSpec
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

/**
 * https://www.raywenderlich.com/8574679-annotation-processing-supercharge-your-development
 * https://proandroiddev.com/annotation-processor-say-less-mean-more-b0e23dd9a3e2
 */

@SupportedSourceVersion(SourceVersion.RELEASE_8) // 1
class AdapterProcessor: AbstractProcessor() {
    override fun getSupportedAnnotationTypes(): MutableSet<String> =
        mutableSetOf(AdapterModel::class.java.canonicalName)


    override fun process(p0: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            ?: return false

        if (roundEnv != null){
            roundEnv.getElementsAnnotatedWith(AdapterModel::class.java)
                .forEach{
                    val modelData = getModelData(it)
                    val fileName = "${modelData.modelName}Adapter" // 1
                    FileSpec.builder(modelData.packageName, fileName) // 2
                        .addType(AdapterCodeBuilder(fileName, modelData).build()) // 3
                        .build()
                        .writeTo(File(kaptKotlinGeneratedDir)) // 4
                }

            return true
        }
        return false
    }

    private fun getModelData(element: Element): ModelData {
        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()
        val modelName = element.simpleName.toString()
        val annotation = element.getAnnotation(AdapterModel::class.java)
        val layoutId = annotation.layoutId
        val viewHolderBindingData = element.enclosedElements.mapNotNull {
            val viewHolderBinding = it.getAnnotation(ViewHolderBinding::class.java)
            if (viewHolderBinding == null){
                null
            } else {
                val elementName = it.simpleName.toString()
                val fieldName = elementName.substring(0, elementName.indexOf('$'))
                ViewHolderBindingData(
                    fieldName,
                    viewHolderBinding.viewId
                )
            }
        }
        return ModelData(
            packageName,
            modelName,
            layoutId,
            viewHolderBindingData
        )
    }
    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }
}