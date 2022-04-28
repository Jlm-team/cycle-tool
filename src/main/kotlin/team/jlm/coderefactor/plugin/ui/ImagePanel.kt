package team.jlm.coderefactor.plugin.ui

import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.JScrollPane

class ImagePanel : JScrollPane() {
    override fun paint(g: Graphics) {
        super.paint(g)
        img?.let {
            g.drawImage(it, 0, 0, null)
        }
    }

    companion object {
        var img: BufferedImage? = null
    }
}