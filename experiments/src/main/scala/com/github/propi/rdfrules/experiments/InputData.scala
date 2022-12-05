package com.github.propi.rdfrules.experiments

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.IOUtils

import java.io.{File, FileInputStream, FileOutputStream}

object InputData {

  def getInputTsvDataset(inputDataset: String): String = {
    if (inputDataset.endsWith(".bz2")) {
      val fileExtracted = inputDataset.stripSuffix(".bz2")
      if (!new File(fileExtracted).isFile) {
        println("file uncompressing...")
        val inputS = new BZip2CompressorInputStream(new FileInputStream(inputDataset))
        val outputS = new FileOutputStream(fileExtracted)
        try {
          IOUtils.copyLarge(inputS, outputS)
        } finally {
          inputS.close()
          outputS.close()
        }
      }
      fileExtracted
    } else if (inputDataset.endsWith(".gz")) {
      val fileExtracted = inputDataset.stripSuffix(".gz")
      if (!new File(fileExtracted).isFile) {
        println("file uncompressing...")
        val inputS = new GzipCompressorInputStream(new FileInputStream(inputDataset))
        val outputS = new FileOutputStream(fileExtracted)
        try {
          IOUtils.copyLarge(inputS, outputS)
        } finally {
          inputS.close()
          outputS.close()
        }
      }
      fileExtracted
    } else {
      inputDataset
    }
  }

}
