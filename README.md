The Contiki Operating System  
============================

***
[![Build Status](https://travis-ci.org/contiki-os/contiki.svg?branch=master)](https://travis-ci.org/contiki-os/contiki/branches)

fork本仓库的唯一目的就是为Contiki提供的仿真工具[Cooja](https://github.com/yulincoder/contiki/tree/master/tools/cooja)添加几个微不足道的功能.
  * 可以在Cooja脚本中执行MspCLI(num, command);函数启动指定节点的MspCLI并执行指定命令.
---
存在问题:
  目前新git clone的Cooja不能够启动, 问题的原因是Cooja.java导入的类
  org.contikios.cooja.mspmote.plugins.MspCLI 来自于apps/mspsim工程.
  而msp工程的build需要导入Cooja工程中的类,这样形成了交叉导入,导致Cooja无法build, mspsim
  亦无法build.

  解决方案就是编译Contiki官方提供的原始[Cooja](https://github.com/contiki-os/contiki/tree/master/tools/cooja)并将编译后生成的apps/mspsim/lib/cooja_mspsim.jar拷到新工程中.

  当然,也可以直接拷贝旧工程的apps/mspsim/lib/cooja_mspsim.jar
***

Contiki is an open source operating system that runs on tiny low-power
microcontrollers and makes it possible to develop applications that
make efficient use of the hardware while providing standardized
low-power wireless communication for a range of hardware platforms.

Contiki is used in numerous commercial and non-commercial systems,
such as city sound monitoring, street lights, networked electrical
power meters, industrial monitoring, radiation monitoring,
construction site monitoring, alarm systems, remote house monitoring,
and so on.

For more information, see the Contiki website:

[http://contiki-os.org](http://contiki-os.org)
