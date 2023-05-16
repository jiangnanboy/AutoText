[![License Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![java_vesion](https://img.shields.io/badge/Java-11%2B-green.svg)](requirements.txt)
![version](https://img.shields.io/badge/AutoText-v--1.1.0-blue)

English | [简体中文](README.md)

# AutoText

Intelligent text automatic processing tool

**AutoText**

The main functions of this project include text error correction, picture ocr and table structure recognition.

**Guide**

- [Text-Error-Correction](#Text-Error-Correction)
- [OCR](#OCR)
- [Layout-Detection](#Layout-Detection)
- [Table-Structure-Recognition](#Table-Structure-Recognition)
- [Todo](#Todo)
- [Contact](#Contact)
- [Citation](#Citation)
- [License](#License)
- [Contribute](#Contribute)

## Text-Error-Correction

- The text error correction section is detailed here [jcorrector](https://github.com/jiangnanboy/jcorrector)
- This project mainly includes:
    - error correction based on ngram
        ```
          Corrector corrector = new Corrector();
          String sentence = “少先队员因该为老人让坐”;
          System.out.println(corrector.correct(sentence));
        ```
    - error correction based on deep learning
        ```
          LoadModel.loadOnnxModel();
          String text = "今天新情很好。";
          text = "你找到你最喜欢的工作，我也很高心。";
          text = "是的，当线程不再使用时，该缓冲区将被清理（我昨天实际上对此进行了测试，我可以每5ms发送一个新线程，而不会产生净内存累积，并确认它的rng内存已在gc上清理）。编号7788";
          text = text.toLowerCase();
          BertTokenizer tokenizer = new BertTokenizer();
          MacBert macBert = new MacBert(tokenizer);
          Map<String, OnnxTensor> inputTensor = null;
          try {
              inputTensor = macBert.parseInputText(text);
          } catch (Exception e) {
              e.printStackTrace();
          }
          List<String> predTokenList = macBert.predCSC(inputTensor);
          predTokenList = predTokenList.stream().map(token -> token.replace("##", "")).collect(Collectors.toList());
          String predString = String.join("", predTokenList);
          System.out.println(predString);
          List<Pair<String, String>> resultList = macBert.getErrors(predString, text);
          for(Pair<String, String> result : resultList) {
              System.out.println(text + " => " + result.getLeft() + " " + result.getRight());
          }
        ```
    - error correction based on template Chinese grammar 
        ```
         String templatePath = GecDemo.class.getClassLoader().getResource(PropertiesReader.get("template")).getPath().replaceFirst("/", "");
         GecCheck gecRun = new GecCheck();
         gecRun.init(templatePath);
         String sentence = "爸爸看完小品后忍俊不禁笑了起来。";
         String infoStr = gecRun.checkCorrect(sentence);
         if(StringUtils.isNotBlank(infoStr)) {
             System.out.println(infoStr);
         }
        ```
    - error correction based on idiom、proper name
        ```
         String properNamePath = ProperDemo.class.getClassLoader().getResource(PropertiesReader.get("proper_name_path")).getPath().replaceFirst("/", "");
         String strokePath = ProperDemo.class.getClassLoader().getResource(PropertiesReader.get("stroke_path")).getPath().replaceFirst("/", "");;
         ProperCorrector properCorrector = new ProperCorrector(properNamePath, strokePath);
         List<String> testLine = List.of(
                 "报应接中迩来",
                 "这块名表带带相传",
                 "这块名表代代相传",
                 "他贰话不说把牛奶喝完了",
                 "这场比赛我甘败下风",
                 "这场比赛我甘拜下封",
                 "这家伙还蛮格尽职守的",
                 "报应接中迩来",  // 接踵而来
                 "人群穿流不息",
                 "这个消息不径而走",
                 "这个消息不胫儿走",
                 "眼前的场景美仑美幻简直超出了人类的想象",
                 "看着这两个人谈笑风声我心理不由有些忌妒",
                 "有了这一番旁证博引",
                 "有了这一番旁针博引",
                 "这群鸟儿迁洗到远方去了",
                 "这群鸟儿千禧到远方去了",
                 "美国前总统特琅普给普京点了一个赞，特朗普称普金做了一个果断的决定"
         );
         for(String line : testLine) {
             System.out.println(properCorrector.correct(line));
         }
        ```
- See the examples/correct section of this project for specific use

## OCR
- This part mainly uses the detection and recognition part in [paddleocr](https://github.com/PaddlePaddle/PaddleOCR) , and converts the model to ONNX format for calling, this project preprocesses the picture before recognition, so that in the CPU environment, the average image recognition time is about 10 seconds.
- See the examples/ocr/text/OcrDemo section of this project for specific use
- PS
    - [Download-Model(BaiduPan)](https://pan.baidu.com/s/1Nn-wO5NdL7FmSAZH6msGqA)
    - Password：b5vq
    - Place model in resources/text_recog or other position
- Usage
``` java
    // read image file
    String imagePath = "examples\\ocr\\img_test\\text_example.png";
    var imageFile = Paths.get(imagePath);
    var image = ImageFactory.getInstance().fromFile(imageFile);
    
    // init model
    String detectionModelFile = OcrDemo.class.getClassLoader().getResource(PropertiesReader.get("text_recog_det_model_path")).getPath().replaceFirst("/", "");
    String recognitionModelFile = OcrDemo.class.getClassLoader().getResource(PropertiesReader.get("text_recog_rec_model_path")).getPath().replaceFirst("/", "");
    Path detectionModelPath = Paths.get(detectionModelFile);
    Path recognitionModelPath = Paths.get(recognitionModelFile);
    OcrApp ocrApp = new OcrApp(detectionModelPath, recognitionModelPath);
    ocrApp.init();
    
    // predict result and consume time
    var timeInferStart = System.currentTimeMillis();
    Pair<List<TextListBox>, Image> imagePair = ocrApp.ocrImage(image, 960);
    System.out.println("consume time: " + (System.currentTimeMillis() - timeInferStart)/1000.0 + "s");
    for (var result : imagePair.getLeft()) {
            System.out.println(result);
    }
    // save ocr result image
    ocrApp.saveImageOcrResult(imagePair, "ocr_result.png", "examples\\ocr\\output");
    ocrApp.closeAllModel();
```
- Result，text and coordinate
```
    position: [800.0, 609.0, 877.0, 609.0, 877.0, 645.0, 800.0, 645.0], text: 8.23%
    position: [433.0, 607.0, 494.0, 607.0, 494.0, 649.0, 433.0, 649.0], text: 68.4
    position: [96.0, 610.0, 316.0, 611.0, 316.0, 641.0, 96.0, 640.0], text: 股东权益比率（%）
    position: [624.0, 605.0, 688.0, 605.0, 688.0, 650.0, 624.0, 650.0], text: 63.2
    position: [791.0, 570.0, 887.0, 570.0, 887.0, 600.0, 791.0, 600.0], text: -39.64%
    position: [625.0, 564.0, 687.0, 564.0, 687.0, 606.0, 625.0, 606.0], text: 49.7
    position: [134.0, 568.0, 279.0, 568.0, 279.0, 598.0, 134.0, 598.0], text: 毛利率（%）
    ......
```
- Result display[OCR](src/main/java/examples/ocr/output/README.md)

## Layout-Detection
- Developed by yolov8, [layout_analysis4j](https://github.com/jiangnanboy/layout_analysis4j)
- See the examples/ocr/layout_detection/LayoutDetection section of this project for specific use
- Usage
``` java
    public static void main(String...args) {
                String modelPath = LayoutDetection.class.getClassLoader().getResource(PropertiesReader.get("model_path")).getPath().replaceFirst("/", "");
                String labelPath = LayoutDetection.class.getClassLoader().getResource(PropertiesReader.get("table_det_labels_path")).getPath().replaceFirst("/", "");
                String imgPath = "D:\\project\\idea_workspace\\layout_analysis4j\\img\\test.webp";
        
                try {
                    LayoutDet modelDet = new LayoutDet(modelPath, labelPath);
                    Mat img = Imgcodecs.imread(imgPath);
                    if (img.dataAddr() == 0) {
                        System.out.println("Could not open image: " + imgPath);
                        System.exit(1);
                    }
                    // run detection
                    try {
                        List<Detection> detectionList = modelDet.detectObjects(img);
        
                        LayoutDetectionUtil.drawPredictions(img, detectionList);
                        System.out.println(JSON.toJSONString(detectionList));
                        Imgcodecs.imwrite("D:\\project\\idea_workspace\\layout_analysis4j\\img\\prediction.jpg", img);
                    } catch (OrtException ortException) {
                        ortException.printStackTrace();
                    }
        
                } catch (OrtException e) {
                    e.printStackTrace();
                }
            }
```
- Result
         
```
    [{"bbox":[137.88228,40.05045,352.5302,60.206684],"confidence":0.9228547,"label":"Header","labelIndex":0},
    {"bbox":[25.661982,52.15992,80.54977,60.164627],"confidence":0.8484751,"label":"Header","labelIndex":0},
    {"bbox":[400.68176,50.069782,462.38123,58.523815],"confidence":0.83252084,"label":"Header","labelIndex":0},
    {"bbox":[27.056168,478.51273,205.72672,656.39886],"confidence":0.9614719,"label":"Text","labelIndex":1},
    {"bbox":[25.820251,304.84778,463.41486,386.2965],"confidence":0.89359975,"label":"Text","labelIndex":1},
    {"bbox":[21.327255,190.66518,463.8985,257.07446],"confidence":0.8879021,"label":"Text","labelIndex":1},
    {"bbox":[182.88458,142.3864,308.64737,156.58653],"confidence":0.79081506,"label":"Text","labelIndex":1},
    {"bbox":[38.471603,435.21515,463.1955,474.5235],"confidence":0.77674204,"label":"Text","labelIndex":1},
    {"bbox":[153.92957,160.85332,338.4781,168.90303],"confidence":0.764402,"label":"Text","labelIndex":1},
    {"bbox":[27.318249,661.32355,151.53812,670.04987],"confidence":0.3412643,"label":"Text","labelIndex":1},
    {"bbox":[306.27896,667.1539,362.94162,674.0262],"confidence":0.8710417,"label":"Figure caption","labelIndex":3},
    {"bbox":[213.9415,479.61642,468.25687,661.9558],"confidence":0.9372132,"label":"Figure","labelIndex":4},
    {"bbox":[26.771957,405.50818,94.59786,416.935],"confidence":0.91822684,"label":"Title","labelIndex":7},
    {"bbox":[131.77039,103.47922,359.43063,120.83272],"confidence":0.88686645,"label":"Title","labelIndex":7},
    {"bbox":[26.655102,661.2926,151.92046,670.0917],"confidence":0.87808716,"label":"Title","labelIndex":7},
    {"bbox":[27.927279,275.91486,68.040955,287.49615],"confidence":0.8072859,"label":"Title","labelIndex":7},
    {"bbox":[27.4192,661.2635,151.4754,670.21075],"confidence":0.49235547,"label":"Footer","labelIndex":8}]
```
- Result display[Layout-Detection](src/main/java/examples/ocr/output/README.md)

## Table-Structure-Recognition
- Developed by opencv based on rules, the main table types identified are: bounded table, unbounded table and partially bounded table
- See the examples/ocr/table/TableDemo section of this project for specific use
- Usage
``` java
    public static void borderedRecog() {
            String imagePath = "D:\\project\\idea_workspace\\AutoText\\src\\main\\java\\examples\\ocr\\img_test\\bordered_example.png";
            Mat imageMat = imread(imagePath);
            System.out.println("imageMat : " + imageMat.size().height() + " " + imageMat.size().width() + " ");
            List<List<List<Integer>>> resultList = BorderedRecog.recognizeStructure(imageMat);
            System.out.println(resultList);
    //        ImageUtils.imshow("Image", pair.getRight());
        }
    
    public static void unBorderedRecog() {
        String imagePath = "D:\\project\\idea_workspace\\AutoText\\src\\main\\java\\examples\\ocr\\img_test\\unbordered_example.jpg";
        Mat imageMat = imread(imagePath);
        System.out.println("imageMat : " + imageMat.size().height() + " " + imageMat.size().width() + " ");
        List<List<List<Integer>>> resultList = UnBorderedRecog.recognizeStructure(imageMat);
        System.out.println(resultList);
//        ImageUtils.imshow("Image", pair.getRight());
    }

    public static void partiallyBorderedRecog() {
        String imagePath = "D:\\project\\idea_workspace\\AutoText\\src\\main\\java\\examples\\ocr\\img_test\\partially_example.jpg";
        Mat imageMat = imread(imagePath);
        System.out.println("imageMat : " + imageMat.size().height() + " " + imageMat.size().width() + " ");
        List<List<List<Integer>>> resultList = PartiallyBorderedRecog.recognizeStructure(imageMat);
        System.out.println(resultList);
//        ImageUtils.imshow("Image", pair.getRight());
    }
```
- Result，table cell coordinate
         
```
    [[[58, 48, 247, 182], [560, 48, 247, 182], [811, 48, 246, 182], [309, 48, 247, 182], [1312, 48, 247, 182], 
    [1061, 48, 247, 182]], [[58, 234, 247, 118], [309, 234, 247, 118], [1061, 234, 247, 118], [560, 234, 247, 118], 
    [811, 234, 246, 118], [1312, 234, 247, 118]], [[58, 356, 247, 118], [309, 356, 247, 118], [560, 356, 247, 118], 
    [811, 356, 246, 118], [1061, 356, 247, 118], [1312, 356, 247, 118]], [[58, 478, 247, 118], [309, 478, 247, 118],
    [560, 478, 247, 118], [811, 478, 246, 118], [1061, 478, 247, 118], [1312, 478, 247, 118]], [[58, 600, 247, 119],
    [309, 600, 247, 119], [560, 600, 247, 119], [811, 600, 246, 119], [1061, 600, 247, 119], [1312, 600, 247, 119]], 
    [[58, 723, 247, 118], [309, 723, 247, 118], [560, 723, 247, 118], [1061, 723, 247, 118], [1312, 723, 247, 118], 
    [811, 723, 246, 118]], [[58, 845, 247, 118], [309, 845, 247, 118], [560, 845, 247, 118], [811, 845, 246, 118], 
    [1312, 845, 247, 118], [1061, 845, 247, 118]]]
```
- Result display[Table Structure](src/main/java/examples/ocr/output/README.md)

#### Table-Structure-Recognition and OCR
- This section will integrate table structure and OCR recognition, identifying both table cells and OCR text.
- See the examples/ocr/table_text/TableTextDemo section of this project for specific use
- Usage
``` java
    public static void main(String...args) throws IOException, TranslateException {
            String imagePath = "D:\\project\\idea_workspace\\AutoText\\src\\main\\java\\examples\\ocr\\img_test\\bordered_example.png";
            TableText tableText = new TableText();
            /**
             * maxSideLen:image resize
             *
             * borderType:{0:all, 1:bordered(default), 2:unbordered, 3:partiallybordered}
             */
            int maxSideLen = -1; // default, no resize
            int borderType = 1; // default, bordered
            List<TextListBox> listBoxes = tableText.tableTextRecog(imagePath);
            for(TextListBox textListBox : listBoxes) {
                System.out.print(textListBox);
            }
        }
```
- Result，table cell coordinate and text
```
    position: [58.0, 48.0, 305.0, 48.0, 305.0, 230.0, 58.0, 230.0], text: 节次 星期
    position: [309.0, 48.0, 556.0, 48.0, 556.0, 230.0, 309.0, 230.0], text: 周一
    position: [811.0, 48.0, 1057.0, 48.0, 1057.0, 230.0, 811.0, 230.0], text: 周三
    position: [1061.0, 48.0, 1308.0, 48.0, 1308.0, 230.0, 1061.0, 230.0], text: 周四
    position: [560.0, 48.0, 807.0, 48.0, 807.0, 230.0, 560.0, 230.0], text: 周二
    position: [1312.0, 48.0, 1559.0, 48.0, 1559.0, 230.0, 1312.0, 230.0], text: 周五
    position: [58.0, 234.0, 305.0, 234.0, 305.0, 352.0, 58.0, 352.0], text: 
    position: [309.0, 234.0, 556.0, 234.0, 556.0, 352.0, 309.0, 352.0], text: 语文
    position: [811.0, 234.0, 1057.0, 234.0, 1057.0, 352.0, 811.0, 352.0], text: 英语
    position: [560.0, 234.0, 807.0, 234.0, 807.0, 352.0, 560.0, 352.0], text: 英语
    position: [1061.0, 234.0, 1308.0, 234.0, 1308.0, 352.0, 1061.0, 352.0], text: 自然
    position: [1312.0, 234.0, 1559.0, 234.0, 1559.0, 352.0, 1312.0, 352.0], text: 数学
    position: [58.0, 356.0, 305.0, 356.0, 305.0, 474.0, 58.0, 474.0], text: 3
    position: [560.0, 356.0, 807.0, 356.0, 807.0, 474.0, 560.0, 474.0], text: 英语
    position: [309.0, 356.0, 556.0, 356.0, 556.0, 474.0, 309.0, 474.0], text: 语文
    position: [811.0, 356.0, 1057.0, 356.0, 1057.0, 474.0, 811.0, 474.0], text: 英语
    position: [1312.0, 356.0, 1559.0, 356.0, 1559.0, 474.0, 1312.0, 474.0], text: 数学
    position: [1061.0, 356.0, 1308.0, 356.0, 1308.0, 474.0, 1061.0, 474.0], text: 语文
    position: [58.0, 478.0, 305.0, 478.0, 305.0, 596.0, 58.0, 596.0], text: 三
    position: [309.0, 478.0, 556.0, 478.0, 556.0, 596.0, 309.0, 596.0], text: 数学
    position: [560.0, 478.0, 807.0, 478.0, 807.0, 596.0, 560.0, 596.0], text: 语文
    position: [811.0, 478.0, 1057.0, 478.0, 1057.0, 596.0, 811.0, 596.0], text: 数学
    position: [1312.0, 478.0, 1559.0, 478.0, 1559.0, 596.0, 1312.0, 596.0], text: 英语
    position: [1061.0, 478.0, 1308.0, 478.0, 1308.0, 596.0, 1061.0, 596.0], text: 语文
    position: [58.0, 600.0, 305.0, 600.0, 305.0, 719.0, 58.0, 719.0], text: 四
    position: [309.0, 600.0, 556.0, 600.0, 556.0, 719.0, 309.0, 719.0], text: 数学
    position: [811.0, 600.0, 1057.0, 600.0, 1057.0, 719.0, 811.0, 719.0], text: 数学
    position: [560.0, 600.0, 807.0, 600.0, 807.0, 719.0, 560.0, 719.0], text: 语文
    position: [1061.0, 600.0, 1308.0, 600.0, 1308.0, 719.0, 1061.0, 719.0], text: 体育
    position: [1312.0, 600.0, 1559.0, 600.0, 1559.0, 719.0, 1312.0, 719.0], text: 英语
    position: [58.0, 723.0, 305.0, 723.0, 305.0, 841.0, 58.0, 841.0], text: 五
    position: [560.0, 723.0, 807.0, 723.0, 807.0, 841.0, 560.0, 841.0], text: 思想品德
    position: [309.0, 723.0, 556.0, 723.0, 556.0, 841.0, 309.0, 841.0], text: 体育
    position: [1061.0, 723.0, 1308.0, 723.0, 1308.0, 841.0, 1061.0, 841.0], text: 数学
    position: [1312.0, 723.0, 1559.0, 723.0, 1559.0, 841.0, 1312.0, 841.0], text: 手工
    position: [811.0, 723.0, 1057.0, 723.0, 1057.0, 841.0, 811.0, 841.0], text: 语文
    position: [58.0, 845.0, 305.0, 845.0, 305.0, 963.0, 58.0, 963.0], text: 六
    position: [309.0, 845.0, 556.0, 845.0, 556.0, 963.0, 309.0, 963.0], text: 美术
    position: [560.0, 845.0, 807.0, 845.0, 807.0, 963.0, 560.0, 963.0], text: 音乐
    position: [1061.0, 845.0, 1308.0, 845.0, 1308.0, 963.0, 1061.0, 963.0], text: 数学
    position: [811.0, 845.0, 1057.0, 845.0, 1057.0, 963.0, 811.0, 963.0], text: 语文
    position: [1312.0, 845.0, 1559.0, 845.0, 1559.0, 963.0, 1312.0, 963.0], text: 写字
```

## Todo

- [x] Add jcorrector for text error correction
- [x] Based on the paddleocr model, image ocr is implemented using java
- [x] Use opencv to identify table structures based on rules
- [x] Integrate form recognition and OCR recognition
- [x] Add layout detection analysis
      
...

## Contact

1、Github：https://github.com/jiangnanboy

2、Blog：https://www.cnblogs.com/little-horse/

3、Email:2229029156@qq.com

## Citation

If you used AutoText in your research, reference it in the following format:

```latex
@{AutoText,
  author = {Shi Yan},
  title = {AutoText: Text automatic processing tool},
  year = {2023},
  url = {https://github.com/jiangnanboy/AutoText},
}
```

## License

**Apache License 2.0**

## Contribute

Interested friends are welcome to fork, star and submit PR.

