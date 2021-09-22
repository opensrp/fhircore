package org.smartregister.fhircore.engine.cql

import ca.uhn.fhir.context.FhirContext
import android.content.res.AssetManager
import org.smartregister.fhircore.engine.cql.CQLAssetBundler
import org.hl7.fhir.instance.model.api.IBaseBundle
import kotlin.Throws
import org.hl7.fhir.instance.model.api.IBaseResource
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory
import ca.uhn.fhir.rest.api.BundleLinks
import ca.uhn.fhir.context.api.BundleInclusionRule
import ca.uhn.fhir.model.valueset.BundleTypeEnum
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.util.BundleUtil
import org.hl7.fhir.r4.model.Bundle
import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException
import java.util.ArrayList

object CQLAssetBundler {
    private var contentBundle: Bundle? = null
    private var dataBundle: Bundle? = null
    private var xml: IParser? = null
    private var json: IParser? = null
    @JvmStatic
    @Synchronized
    fun getContentBundle(fhirContext: FhirContext, assetManager: AssetManager): Bundle? {
        if (contentBundle == null) {
            contentBundle =
                generateAssetBundle(fhirContext, assetManager, "resources/content") as Bundle
        }
        return contentBundle
    }

    @JvmStatic
    @Synchronized
    fun getDataBundle(fhirContext: FhirContext, assetManager: AssetManager): Bundle? {
        if (dataBundle == null) {
            dataBundle = generateAssetBundle(fhirContext, assetManager, "resources/tests") as Bundle
        }
        return dataBundle
    }

    private fun generateAssetBundle(
        fhirContext: FhirContext,
        assetManager: AssetManager,
        root: String
    ): IBaseBundle {
        var files: List<String>? = null
        files = try {
            recurse(assetManager, root, assetManager.list(root))
        } catch (e: IOException) {
            throw RuntimeException("Unable to generate asset bundle", e)
        }
        return bundleFiles(fhirContext, assetManager, root, files)
    }

    @Throws(IOException::class)
    private fun recurse(
        assetManager: AssetManager,
        root: String,
        files: Array<String>?
    ): List<String> {
        val returnFiles: MutableList<String> = ArrayList()
        for (f in files!!) {
            val fullPath = "$root/$f"
            if (f.endsWith(".xml") || f.endsWith(".json")) {
                returnFiles.add(fullPath)
            } else {
                returnFiles.addAll(recurse(assetManager, fullPath, assetManager.list(fullPath)))
            }
        }
        return returnFiles
    }

    private fun bundleFiles(
        fhirContext: FhirContext,
        assetManager: AssetManager,
        root: String,
        files: List<String>?
    ): IBaseBundle {
        val resources: MutableList<IBaseResource> = ArrayList()
        for (f in files!!) {
            if (!f.endsWith(".xml") && !f.endsWith(".json")) {
                continue
            }
            val resource = parseFile(assetManager, fhirContext, f) ?: continue
            if (resource is IBaseBundle) {
                val innerResources = flatten(fhirContext, resource)
                resources.addAll(innerResources)
            } else {
                resources.add(resource)
            }
        }
        val bundleFactory = fhirContext.newBundleFactory()
        val bundleLinks = BundleLinks(root, null, true, BundleTypeEnum.COLLECTION)
        bundleFactory.addRootPropertiesToBundle(
            "bundled-directory",
            bundleLinks,
            resources.size,
            null
        )
        bundleFactory.addResourcesToBundle(
            resources, BundleTypeEnum.COLLECTION, "",
            BundleInclusionRule.BASED_ON_INCLUDES, null
        )
        return bundleFactory.resourceBundle as IBaseBundle
    }

    private fun parseFile(
        assetManager: AssetManager,
        fhirContext: FhirContext,
        f: String
    ): IBaseResource? {
        return try {
            val resource = assetManager.open(f)
            val selectedParser = selectParser(fhirContext, f)
            selectedParser!!.parseResource(resource)
        } catch (e: Exception) {
            null
        }
    }

    private fun selectParser(fhirContext: FhirContext, filename: String): IParser? {
        return if (filename.toLowerCase().endsWith("json")) {
            if (json == null) {
                json = fhirContext.newJsonParser()
            }
            json
        } else {
            if (xml == null) {
                xml = fhirContext.newXmlParser()
            }
            xml
        }
    }

    private fun flatten(fhirContext: FhirContext, bundle: IBaseBundle): List<IBaseResource> {
        val resources: MutableList<IBaseResource> = ArrayList()
        val bundleResources = BundleUtil.toListOfResources(fhirContext, bundle)
        for (r in bundleResources) {
            if (r is IBaseBundle) {
                val innerResources = flatten(fhirContext, r)
                resources.addAll(innerResources)
            } else {
                resources.add(r)
            }
        }
        return resources
    }
}