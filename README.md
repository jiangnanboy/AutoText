[![License Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![java_vesion](https://img.shields.io/badge/Java-11%2B-green.svg)](requirements.txt)
![version](https://img.shields.io/badge/AutoText-v--1.1.0-blue)

[English](README.en.md) | 简体中文

# AutoText

智能文本自动处理工具（Intelligent text automatic processing tool）。

**AutoText**的功能主要有文本纠错，图片ocr以及表格结构识别等。

**Guide**

- [文本纠错](#文本纠错)
- [图片ocr](#图片ocr)
- [表格结构识别](#表格结构识别)
- [Todo](#Todo)
- [Contact](#Contact)
- [Citation](#Citation)
- [License](#License)
- [Contribute](#Contribute)
- [Reference](#reference)

## 文本纠错

- 文本纠错部分详细见[jcorrector](https://github.com/jiangnanboy/jcorrector)
- 本项目主要有基于ngram的纠错、基于深度学习的纠错、基于模板中文语法纠错以及成语、专名纠错等
- 具体使用见本项目中的examples/correct部分

## 图片ocr
- 这部分主要利用[paddleocr](https://github.com/PaddlePaddle/PaddleOCR) 中的检测与识别部分，并将其中模型转为onnx格式进行调用，本项目在识别前对图片进行了预处理，使得在cpu环境下，平均一张图10秒左右。
- 具体使用见本项目中的examples/ocr/text/OcrDemo部分
- PS
    - [模型网盘下载](https://pan.baidu.com/s/1Nn-wO5NdL7FmSAZH6msGqA)
    - 提取码：b5vq
    - 模型下载后可放入resources的text_recgo下或其它位置

- 使用
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
- 结果，为文字及其坐标
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
- 结果展示

<br/><br/> 
<p align="center">
  <a>
    <img src="src/main/java/examples/ocr/output/ocr_result.png">
  </a>
</p>
<br/><br/>

## 表格结构识别
- 基于规则由opencv研发，主要识别的表格类型有：有边界表格、无边界表格以及部分有边界表格。
- 具体使用见本项目中的examples/ocr/table/TableDemo部分
- 使用
``` java
    public static void borderedRecog() {
        String imagePath = "examples\\ocr\\img_test\\bordered_example.png";
        Mat imageMat = imread(imagePath);
        System.out.println("imageMat : " + imageMat.size().height() + " " + imageMat.size().width() + " ");
        Pair< List<List<List<Integer>>>, Mat> pair = BorderedRecog.recognizeStructure(imageMat);
        System.out.println(pair.getLeft());
        ImageUtils.imshow("Image", pair.getRight());
    }

    public static void unBorderedRecog() {
        String imagePath = "examples\\ocr\\img_test\\unbordered_example.jpg";
        Mat imageMat = imread(imagePath);
        System.out.println("imageMat : " + imageMat.size().height() + " " + imageMat.size().width() + " ");
        Pair< List<List<List<Integer>>>, Mat> pair = UnBorderedRecog.recognizeStructure(imageMat);
        System.out.println(pair.getLeft());
        ImageUtils.imshow("Image", pair.getRight());
    }

    public static void partiallyBorderedRecog() {
        String imagePath = "examples\\ocr\\img_test\\partially_example.jpg";
        Mat imageMat = imread(imagePath);
        System.out.println("imageMat : " + imageMat.size().height() + " " + imageMat.size().width() + " ");
        Pair< List<List<List<Integer>>>, Mat> pair = PartiallyBorderedRecog.recognizeStructure(imageMat);
        System.out.println(pair.getLeft());
        ImageUtils.imshow("Image", pair.getRight());
    }
```
- 结果，为表格单元格坐标
```
    [[[58, 48, 247, 182], [309, 48, 247, 182], [560, 48, 247, 182], [], [], [1061, 48, 247, 182], [1312, 48, 247, 182], 
    [811, 48, 246, 182], [], [], [], []], [[58, 234, 247, 118], [309, 234, 247, 118], [560, 234, 247, 118], [], [811, 234, 246, 118], 
    [], [1061, 234, 247, 118], [], [], [1312, 234, 247, 118], [], []], [[58, 356, 247, 118], [], [309, 356, 247, 118], 
    [560, 356, 247, 118], [], [811, 356, 246, 118], [], [], [1061, 356, 247, 118], [], [1312, 356, 247, 118], []], [[58, 478, 247, 118],
    [309, 478, 247, 118], [], [560, 478, 247, 118], [811, 478, 246, 118], [], [], [1312, 478, 247, 118], [], [1061, 478, 247, 118], [], []],
    [[58, 600, 247, 119], [309, 600, 247, 119], [], [811, 600, 246, 119], [560, 600, 247, 119], [1061, 600, 247, 119], [],
    [1312, 600, 247, 119], [], [], [], []], [[58, 723, 247, 118], [], [309, 723, 247, 118], [811, 723, 246, 118], [560, 723, 247, 118],
    [], [], [1061, 723, 247, 118], [], [1312, 723, 247, 118], [], []], [[58, 845, 247, 118], [309, 845, 247, 118], [], [], 
    [811, 845, 246, 118], [560, 845, 247, 118], [], [1312, 845, 247, 118], [], [1061, 845, 247, 118], [], []]]
```
- 结果展示

<br/><br/> 
<p align="center">
  <a>
    <img src="src/main/java/examples/ocr/output/bordered_example_result.png">
  </a>
</p>
<br/><br/>

<br/><br/> 
<p align="center">
  <a>
    <img src="src/main/java/examples/ocr/output/unbordered_example_result.png">
  </a>
</p>
<br/><br/>

<br/><br/> 
<p align="center">
  <a>
    <img src="src/main/java/examples/ocr/output/partially_example_result.png">
  </a>
</p>
<br/><br/>

## Todo

- [x] 加入jcorrector文本纠错，修改部分程序
- [x] 基于paddleocr模型，利用java实现图片ocr
- [x] 基于规则利用opencv识别表格结构

...

## Contact

1、github：https://github.com/jiangnanboy

2、博客：https://www.cnblogs.com/little-horse/

3、邮件:2229029156@qq.com

## Citation

如果你在研究中使用了AutoText，请按如下格式引用：

```latex
@{jcorrector,
  author = {Shi Yan},
  title = {AutoText: Text automatic processing tool},
  year = {2023},
  url = {https://github.com/jiangnanboy/AutoText},
}
```

## License

AutoText 的授权协议为 **Apache License 2.0**，可免费用做商业用途。请在产品说明中附加AutoText的链接和授权协议。AutoText受版权法保护，侵权必究。

## Contribute

欢迎有兴趣的朋友fork，提交PR。

## Reference
* https://github.com/jiangnanboy/jcorrector
* https://github.com/PaddlePaddle/PaddleOCR
* https://github.com/bytedeco/javacv
* https://github.com/deepjavalibrary/djl
* https://github.com/PaddlePaddle/Paddle
* https://github.com/microsoft/onnxruntime
* https://github.com/mymagicpower/AIAS
* https://github.com/jiangnanboy/java-springboot-paddleocr-v2
* https://github.com/jiangnanboy/java-springboot-paddleocr
* https://github.com/jiangnanboy/model2onnx
* https://github.com/jiangnanboy/macbert-java-onnx
* https://github.com/jiangnanboy/onnx-java
