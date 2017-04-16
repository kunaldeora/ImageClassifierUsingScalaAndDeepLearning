package com.neu.yelp.main

import com.neu.yelp.preprocessing.{ImageUtils}
import com.neu.yelp.cnn.TrainCNN.trainModel
import com.neu.yelp.postprocessing.TransformData
import com.neu.yelp.cnn.PredictCNN.doPredictionForLabel
import org.deeplearning4j.ui.api.UIServer
import com.neu.yelp.preprocessing.Csv2Map.{getUniqueBizIDForTest, photoToBizId2Map, bizToLabel2Map}

/**
  * Created by Pranay on 3/23/2017
  * Modified by Kunal on 4/11/2017
  * Modified by Manasi on 4/15/2017
  */
object Main{

  def main(args: Array[String]): Unit = {

    /*** TRAINING & TESTING ***/
    println(" 1) Starting the training and testing phase....")

    // read the business ids and their lables and generate the a map of business ids and list of labels
    val biz2LabelMap = bizToLabel2Map("..\\Input_Datasets\\filtered_train_biz_ids.csv")
    println("biz2LabelMap : " + biz2LabelMap.size)

    // read the image ids and their business ids and generate the a map of image ids and business ids
    val image2BizMap = photoToBizId2Map("..\\Input_Datasets\\train_photo_to_biz_ids.csv", biz2LabelMap.keySet.toList)
    println("image2BizMap : " + image2BizMap.size)

    // read the image vector data and generate the a map of image ids and image vector data
    // This step involves prepreprocessing of images - 1) Resizing 2) Grayling 3) Pixelating 128 x 128
    val img2DataMap = ImageUtils.img2Map("..\\..\\Input_Datasets\\train_photos",image2BizMap);
    println("img2DataMap : " + img2DataMap.size)

    // transform data is our input dataset, i.e. [image_id, business_id, image_vector_data, business_label]
    val transformedData = new TransformData(img2DataMap,image2BizMap,biz2LabelMap,"train_test")
    println("transformedData generated lazyly...")

    // start the UI Server to monitor the model training
    // localhost:9090/train
    println("Starting the training UI on localhost:9090/train ")
    val uIServer = UIServer.getInstance()

    // train the model for each business label on the transformed data and save the model under results folder
    val cnnModel1= trainModel(transformedData, bizLabel = 1, "..\\Output_Models\\models_1", uIServer)
    /*val cnnModel0= trainModel(transformedData, bizLabel = 0, "..\\Output_Models\\models_0", uIServer)
    val cnnModel2= trainModel(transformedData, bizLabel = 2, "..\\Output_Models\\models_2", uIServer)
    val cnnModel3= trainModel(transformedData, bizLabel = 3, "..\\Output_Models\\models_3", uIServer)
    val cnnModel4= trainModel(transformedData, bizLabel = 4, "..\\Output_Models\\models_4", uIServer)
*/

    /*** PREDICTION ***/
    println(" 2) Starting the prediction phase....")
    // fetch the list of business id for which prediction will be performed
    val unpredictedBizIds = getUniqueBizIDForTest("..\\Input_Datasets\\unpredicted_biz_ids.csv")
    println("unpredictedBizIds : " + unpredictedBizIds.size)

    // read the image ids for the unpredicted business ids and generate the a map of image ids and business ids
    val unpredictedImg2BizIdsMap = photoToBizId2Map("..\\Input_Datasets\\validate_train_photo_to_biz_ids.csv", unpredictedBizIds)
    println("unpredictedImg2BizIdsMap : " + unpredictedImg2BizIdsMap.size)

    // read the image vector data and generate the a map of image ids and image vector data
    // This step involves prepreprocessing of images - 1) Resizing 2) Grayling 3) Pixelating 128 x 128
    val unpredictedImg2DataMap = ImageUtils.img2Map("..\\..\\Input_Datasets\\train_photos",unpredictedImg2BizIdsMap)
    println("unpredictedImg2DataMap : " + unpredictedImg2DataMap.size)

    // transform data is our input dataset, i.e. [image_id, business_id, image_vector_data, business_label]
    // for unpredicted business ids , the labels are initally empty so we will pass null
    val transformedDataTest = new TransformData(unpredictedImg2DataMap,unpredictedImg2BizIdsMap, null, "predict")
    println("transformedDataTest generating lazyly....")


    // Make predictions for this transformed test for each business label
    // Run each label model to predict if the label is valid for the business id
    println("Starting the prediction using each label's model....")
    var predictLabel1ForBusinesses = doPredictionForLabel(transformedDataTest, unpredictedBizIds, 1, cnnModel1)
    /*predictLabel1ForBusinesses = predictLabel1ForBusinesses ::: doPredictionForLabel(transformedDataTest, unpredictedBizIds, 0, cnnModel0)
    predictLabel1ForBusinesses = predictLabel1ForBusinesses ::: doPredictionForLabel(transformedDataTest, unpredictedBizIds, 2, cnnModel2)
    predictLabel1ForBusinesses = predictLabel1ForBusinesses ::: doPredictionForLabel(transformedDataTest, unpredictedBizIds, 3, cnnModel3)
    predictLabel1ForBusinesses = predictLabel1ForBusinesses ::: doPredictionForLabel(transformedDataTest, unpredictedBizIds, 4, cnnModel4)*/

    println("Analyzing the predictions.....")
    // Analyse the predicted data and mark the label for the business
    val predictedMap: Map[String,List[Int]] = predictLabel1ForBusinesses.map( s=> (s._1, s._2) )
      .groupBy(_._1)
      .mapValues(_.map(_._2))

    println("Final Predictions :")
    predictedMap.foreach(println)




  }
}
