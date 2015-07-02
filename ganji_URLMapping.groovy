import com.sohu.scs.imgprocess.common.exception.ImgProcessUserException;
import com.sohu.scs.imgprocess.urlmapping.URLMappingProcessor;
import com.sohu.scs.imgprocess.urlmapping.URLMappingTool;

class GanjiURLMapping extends URLMappingProcessor {
    def bucketName = ""
    def modeStr = ""
    def objectName = ""

    // parameters provided by ganji
    def WATERMARKS = [
        'ganji_v4.png',
        'guazi.png',
        'duanzu-1.png',
        'duanzu-2.png',
        '',
        'mayi_you.png',
        'mayi_you_br.png',
        'mayi_you_br_big.png'
    ]

    def CATEGORYS = [
        'default':[0, 3, false],
        'xiaoqu':[0, 3, false],
        'tuan':[4, 3, false],
        'nomask':[4, 3, false],
        'youhuijuan':[4, 3, false],
        'groupon':[4, 3, false],
        'tuiguang/ad':[4, 3, false],
        'tuiguang/mqjob':[4, 3, false],
        'tuiguang/contract':[4, 3, false],
        'company_license':[4, 3, false],
        'guazi':[1, 3, false],
        'guazi_avatar':[4, 3, false],
        'duanzu':[3, 7, true],
        'duanzu_vip':[2, 7, true],
        'tuiguang':[0, 3, false],
        'duanzu_head':[4, 3, false],
        'raw':[4, 3, false],
        'tuiguang/customer':[4, 3, false],
        'tuiguang/idcard':[4, 3, false],
        'tuiguang/business_card':[4, 3, false],
        'mayinomask':[4, 3, false],
        'mayi_you':[5, 7, false],
        'mayi_you_br':[6, 3, false],
        'mayi_you_br_big':[7, 3, false],
        'bangbang_pic':[0, 3, false],
        'zhaopin/renzheng':[0, 5, false],
        'tuiguang/fang':[0, 3, false],
        'secondmarket':[0, 3, false],
        'pet':[0, 3, false],
        'vehicle':[0, 3, false],
        'hospital':[0, 3, false],
        'hospital_nomask':[4, 3, false],
        'ticketing':[0, 3, false],
        'event':[0, 3, false],
        'personals':[0, 3, false],
        'housing':[0, 3, false],
        'training':[0, 3, false],
        'wanted':[0, 3, false],
        'parttime_wanted':[0, 3, false],
        'findjob':[0, 3, false],
    ]

    def DUANZU_CATEGORYS = [
        'duanzu':[350, 350],
        'duanzu_head':[350, 350],
        'duanzu_vip':[350, 350],
        'mayi_you':[350, 350],
        'mayi_you_br':[380, 380],
        'mayi_you_br_big':[380, 380],
    ]

    def JPG_QUALITY = [
        '0':15,
        '1':40,
        '2':50,
        '3':60,
        '4':70,
        '5':75,
        '6':80,
        '7':85,
        '8':90,
        '9':95,
    ]

    String GetMappingURL(String src_url) {
        def finalURL = null
        finalURL = GetInfoFromURL(src_url.substring(1))

        return finalURL
    }

    def GetInfoFromURL(def src_url) {

        def sign = null
        try {
            sign = src_url.indexOf('/')
            if (sign < 0) {
                throw new ImgProcessUserException(
                "URL pattern Error!, Sample like: {bucketName}/{objectName}");
            }
            this.bucketName = src_url[0..sign-1]

            def text =  src_url.substring(sign + 1)
            if (!text) {
                throw new ImgProcessUserException(
                "URL pattern Error!, Sample like: {bucketName}/{objectName}");
            }

            def matcher = text =~ /_(\d+)-(\d+)(c|f)?_(\d{1})-(\d+)\.(\w+)$/
            def func = ""
            def width = ""
            def height = ""
            def mode = ""
            def quality = ""
            def version = ""
            def suffix = ""

            matcher.each { whole, w, h, m, q, v, s ->
                func = whole
                width = w
                height = h
                mode = m
                quality = q
                version = v
                suffix = s
            }

            if (width && height)
            {
                def src_pic = matcher.replaceAll("_0-0_9-0")
                if (suffix)
                    src_pic = src_pic + "." + suffix
                objectName = src_pic
                
                def metas = GetMetadata(this.bucketName, this.objectName)
                def category = metas[0]
                def src_width = metas[1]
                def src_height = metas[2]
                def x = 0
                def y = 0
                // zoom, crop or fill
                width = width.toInteger()
                height = height.toInteger()
                if (width == 0 || height == 0)
                {
                    if (width == 0 && height == 0) {}
                    else if (width == 0)
                    {
                        modeStr += "c_zoom,h_${height}"
                        if (height <= src_height) {
                            src_width = src_width * height / src_height
                            src_height = height
                        }
                    }
                    else if (height == 0)
                    {
                        modeStr += "c_zoom,w_${width}"
                        if (width <= src_width) {
                            src_height = src_height * width / src_width
                            src_width = width  
                        }
                    }
                }
                else
                {
                    if (mode == "c")
                    {
                        // need to crop
                        modeStr += "c_fill,w_${width},h_${height}"
                        def temp_width = src_width
                        def temp_height = src_height
                        if (width * src_height > height * src_width) {
                            temp_height = height * src_width / width
                        } else {
                            temp_width = width * src_height / height
                        }
                        if (height < temp_width) {
                            src_width = width
                            src_height = height
                        }
                    }
                    else if (mode == "f")
                    {
                        modeStr += "c_pad,w_${width},h_${height},red_64,green_64,blue_64" // tianbian
                        src_width = width
                        src_height = height
                    }
                    else
                    {
                        modeStr += "c_fit,w_${width},h_${height}"
                        if (width < src_width || height < src_height)
                        {
                            if (width * src_height > height * src_width) {
                                src_width = src_width * height / src_height
                                src_height = height
                            }
                            else {
                                src_height = src_height * width / src_width
                                src_width = width
                            }
                        }
                    }
                }               
                                
                // watermark
                //println src_width
                //println src_height
                if (CATEGORYS[category] && src_width != -1 && src_height != -1)
                {

                    def category_info = CATEGORYS[category]
                    def index = category_info[0]
                    def position = category_info[1]
                    def sharpen = category_info[2]
                    def watermark_min_width = 0
                    def watermark_min_height = 0

                    if (DUANZU_CATEGORYS[category])
                    {
                        watermark_min_width = DUANZU_CATEGORYS[category][0]
                        watermark_min_height = DUANZU_CATEGORYS[category][1]
                    }
                    else
                    {
                        watermark_min_width = 150
                        watermark_min_height = 150
                    }

                    def watermark_obj = null
                    def direction = null
                    if (src_width > watermark_min_width && src_height > watermark_min_height)
                    {
                        if (WATERMARKS[index] != '')
                        {
                            watermark_obj = WATERMARKS[index]
                                switch (position)
                            {
                                case 3:
                                    if (index == 0 && ((src_width >=150 && src_width <=340) || (src_height >= 100 && src_height <= 220)))
                                    {
                                        watermark_obj = "ganji_v4_small.png"
                                        x = -7
                                        y = -5
                                    }
                                    else
                                    {
                                        x = -10
                                        y = -10
                                    }
                                    direction = "br"
                                    break
                                case 5:
                                    x = -10
                                    y = 10
                                    direction = "tr"
                                    break
                                case 7:
                                    x = 10
                                    y = 10
                                    direction = "tl"
                                    break
                            }
                            modeStr += ",o_watermark,t_pic,d_0,x_${x},y_${y},g_${direction},{${bucketName}:${watermark_obj}}"
                        }                       
                    }
                }
                

                if (JPG_QUALITY[quality])
                {
                    modeStr += ",q_${JPG_QUALITY[quality]}"
                }

                if (modeStr)
                {
                    if (modeStr.startsWith(","))
                    {
                        modeStr = modeStr.substring(1)
                    }
                    return "/" + bucketName + "/" + modeStr + "/" + objectName
                }
                else
                {
                    return "/" + bucketName + "/" + objectName
                }
            }
        } catch (Exception e){
            throw new ImgProcessUserException(e.getMessage())
        }
        return "/" + src_url;
    }

    def GetMetadata(def bucket, def object) {
        //fetch the metaData from scs
        def metaData = URLMappingTool.getObjectMetaData(bucket, object, true, super.getHost())
        if (metaData == null) {
            throw new ImgProcessUserException(
            "get object meta data failed: metaData null!");
        }

        def metas = []
        def category = null
        def width = null
        def height = null

        if (metaData.containsKey("x-scs-meta-category")) {
            category = metaData['x-scs-meta-category'].toString()
        } else {
            category = "default"
        }

        if (metaData.containsKey("x-scs-meta-width")) {
            width = metaData['x-scs-meta-width'].toString().toInteger()
        } else {
            width = -1
        }

        if (metaData.containsKey("x-scs-meta-height")) {
            height = metaData['x-scs-meta-height'].toString().toInteger()
        } else {
            if (metaData.containsKey("x-scs-meta-heigth")) {
                height = metaData['x-scs-meta-heigth'].toString().toInteger()
            } else {
                height = -1
            }
        }

        metas.add(category)
        metas.add(width)
        metas.add(height)

        return metas
    }
}

/*public static void main(def args) {


    假定原图url（即上传时返回的url，以gjfs开头）为：basename.ext，则缩略图的url为：basename_width-height(c|f)?_quality-version.ext
     * width:缩略图的宽度
     * height:缩略图的高度
     * c|f选项：如果原始图片的宽高比和所要求的缩略图宽高比不同是如何处理。
     * 如果不指定选项，那么自动调整缩略图的宽高以保持比例不变。
     * c:裁切原图以适应缩略图的宽高比
     * f:填充图片以适应缩略图的宽高比
     * quality:图片质量，取值0-9.数值越高，图片质量越好
     * version:版本
     * 示例：
     * 原图url：gjfs04/M05/C6/78/wKhzKlGSOZDo8yOyAAOoAN0Lbyo424.jpg
     * 缩略图url：gjfs04/M05/C6/78/wKhzKlGSOZDo8yOyAAOoAN0Lbyo424_500-0_9-0.jpg
     * 用户原始上传图：gjfs04/M05/C6/78/wKhzKlGSOZDo8yOyAAOoAN0Lbyo424_0-0_9-0.jpg
         

    url_mapping_func = new GanjiURLMapping()
    def test_url = """/ganjitest/wKhzV1OB1wyLE3PlAABRxGl0vc8793_400-100f_8-0.jpg"""
    println(url_mapping_func.GetMappingURL(test_url))
}*/
