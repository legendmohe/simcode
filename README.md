# simcode

为了排查项目中重复文件（代码），制作了Java文件相似度检测工具。

本工具基于项目https://dickgrune.com/Programs/similarity_tester/

使用方法：

    java -jar simcode.jar -i 90 E:\job\repo\myproject\src\

其中 -i 表示相似度阈值，默认90，即90以上相似度的结果才会打印出来。开发者可根据结果，对项目进行重构。

注：目前jar仅支持win平台，java文件的相似度比较，
