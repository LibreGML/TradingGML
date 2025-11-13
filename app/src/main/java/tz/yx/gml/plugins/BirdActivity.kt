package tz.yx.gml.plugins

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import kotlin.math.min
import kotlin.random.Random

class BirdActivity : AppCompatActivity() {
    private lateinit var gameView: GameView
    private val handler = Handler(Looper.getMainLooper())
    private val gameRunnable = object : Runnable {
        override fun run() {
            gameView.update()
            gameView.invalidate()
            handler.postDelayed(this, 16)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        gameView = GameView(this)
        setContentView(gameView)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(gameRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(gameRunnable)
    }

    class GameView(context: BirdActivity) : View(context) {
        // 修改配色方案
        private val skyColor = Color.parseColor("#BBDEFB") // 卡通浅蓝色背景
        private val birdColor = Color.parseColor("#FF9800") // 橙色小鸟
        private val birdDetailColor = Color.parseColor("#F57C00") // 小鸟细节
        private val pipeColor = Color.parseColor("#90CAF9") // 普通蓝色管道
        private val pipeDetailColor = Color.parseColor("#0D47A1") // 管道细节
        private val groundColor = Color.parseColor("#82B1FF") // 浅紫色草地
        private val groundDetailColor = Color.parseColor("#42A5F5") // 草地细节
        private val textColor = Color.parseColor("#FFFFFF") // 文字
        private val cloudColor = Color.parseColor("#FFFFFF") // 白色云朵

        // 新增渐变效果颜色
        private val pipeTopColor = Color.parseColor("#64B5F6")
        private val pipeBottomColor = Color.parseColor("#0D47A1")

        private val birdPaint = Paint().apply {
            color = birdColor
            isAntiAlias = true
        }

        private val pipePaint = Paint().apply {
            color = pipeColor
            isAntiAlias = true
        }

        private val backgroundPaint = Paint().apply {
            color = skyColor
            isAntiAlias = true
        }

        private val groundPaint = Paint().apply {
            color = groundColor
            isAntiAlias = true
        }

        private val cloudPaint = Paint().apply {
            color = cloudColor
            isAntiAlias = true
        }

        private val textPaint = Paint().apply {
            color = textColor
            textSize = 80f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        // 游戏元素
        private var birdY = 0f
        private var birdVelocity = 0f
        private val gravity = 0.8f
        private val jumpStrength = -18f
        private val pipeWidth = 250f
        private var pipes = mutableListOf<Pipe>()
        private var gameState = GameState.WAITING
        private var pipeSpeed = 7.5f

        private val pipeGap = 630f
        private val pipeInterval = 3000L
        private var lastPipeTime = 0L
        private val groundHeight = 152f

        // 云朵数据
        private var clouds = mutableListOf<Cloud>()

        private enum class GameState {
            WAITING, PLAYING, GAME_OVER
        }

        data class Pipe(var x: Float, var topHeight: Float, var passed: Boolean = false)
        data class Cloud(var x: Float, var y: Float, var size: Float)

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            birdY = height / 2f

            // 初始化云朵
            clouds.clear()
            for (i in 0..5) {
                clouds.add(
                    Cloud(
                    Random.nextFloat() * width,
                    Random.nextFloat() * (height / 3),
                    80f + Random.nextFloat() * 70f
                )
                )
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // 绘制背景
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

            // 绘制云朵
            drawClouds(canvas)

            // 绘制管道
            for (pipe in pipes) {
                // 顶部管道（渐变效果）
                val topPipePaint = Paint().apply {
                    color = pipeColor
                    isAntiAlias = true
                }
                canvas.drawRect(pipe.x, 0f, pipe.x + pipeWidth, pipe.topHeight, topPipePaint)

                // 顶部管道装饰
                val topDetailPaint = Paint().apply {
                    color = pipeTopColor
                    isAntiAlias = true
                }
                canvas.drawRect(pipe.x + 15, pipe.topHeight - 40, pipe.x + pipeWidth - 15, pipe.topHeight, topDetailPaint)

                // 顶部管道边缘
                val topEdgePaint = Paint().apply {
                    color = pipeDetailColor
                    isAntiAlias = true
                }
                canvas.drawRect(pipe.x, pipe.topHeight - 10, pipe.x + pipeWidth, pipe.topHeight, topEdgePaint)

                // 底部管道
                val bottomPipeY = pipe.topHeight + pipeGap
                val bottomPipePaint = Paint().apply {
                    color = pipeColor
                    isAntiAlias = true
                }
                canvas.drawRect(pipe.x, bottomPipeY, pipe.x + pipeWidth, height - groundHeight, bottomPipePaint)

                // 底部管道装饰
                val bottomDetailPaint = Paint().apply {
                    color = pipeBottomColor
                    isAntiAlias = true
                }
                canvas.drawRect(pipe.x + 15, bottomPipeY, pipe.x + pipeWidth - 15, bottomPipeY + 40, bottomDetailPaint)

                // 底部管道边缘
                val bottomEdgePaint = Paint().apply {
                    color = pipeDetailColor
                    isAntiAlias = true
                }
                canvas.drawRect(pipe.x, bottomPipeY, pipe.x + pipeWidth, bottomPipeY + 10, bottomEdgePaint)

                // 管道内部装饰
                val innerPaint = Paint().apply {
                    color = Color.parseColor("#BBDEFB")
                    isAntiAlias = true
                }
                canvas.drawRect(pipe.x + 30, pipe.topHeight - 30, pipe.x + pipeWidth - 30, pipe.topHeight - 10, innerPaint)
                canvas.drawRect(pipe.x + 30, bottomPipeY + 10, pipe.x + pipeWidth - 30, bottomPipeY + 30, innerPaint)
            }

            // 绘制地面（更丰富的细节）
            canvas.drawRect(0f, height - groundHeight, width.toFloat(), height.toFloat(), groundPaint)

            // 地面装饰线条
            val groundDetailPaint = Paint().apply {
                color = groundDetailColor
                strokeWidth = 5f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }

            for (i in 0..width step 40) {
                val path = Path()
                path.moveTo(i.toFloat(), height - groundHeight + 20)
                path.lineTo(i.toFloat() + 20, height - groundHeight + 40)
                path.lineTo(i.toFloat() + 40, height - groundHeight + 20)
                canvas.drawPath(path, groundDetailPaint)
            }

            // 绘制草丛效果
            val grassPaint = Paint().apply {
                color = Color.parseColor("#81D4FA") // 浅紫色草丛
                isAntiAlias = true
            }

            for (i in 0..width step 20) {
                val grassPath = Path()
                grassPath.moveTo(i.toFloat(), height - groundHeight)
                grassPath.lineTo(i.toFloat() - 5, height - groundHeight - 20)
                grassPath.lineTo(i.toFloat() + 5, height - groundHeight - 15)
                canvas.drawPath(grassPath, grassPaint)
            }

            // 绘制小鸟（更卡通化）
            val birdRadius = min(width, height) / 18f

            // 小鸟身体（渐变效果用深浅橙色）
            canvas.drawCircle(width / 4f, birdY, birdRadius, birdPaint)

            // 小鸟翅膀
            val wingPaint = Paint().apply {
                color = birdDetailColor
                isAntiAlias = true
            }
            canvas.drawArc(
                width / 4f - birdRadius,
                birdY - birdRadius/2,
                width / 4f,
                birdY + birdRadius/2,
                0f,
                180f,
                true,
                wingPaint
            )

            // 小鸟尾巴
            val tailPath = Path()
            tailPath.moveTo(width / 4f - birdRadius, birdY)
            tailPath.lineTo(width / 4f - birdRadius * 1.8f, birdY - birdRadius/1.5f)
            tailPath.lineTo(width / 4f - birdRadius * 1.5f, birdY)
            tailPath.lineTo(width / 4f - birdRadius * 1.8f, birdY + birdRadius/1.5f)
            tailPath.close()
            canvas.drawPath(tailPath, wingPaint)

            // 小鸟眼睛
            val eyePaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
            }
            val pupilPaint = Paint().apply {
                color = Color.BLACK
                isAntiAlias = true
            }
            canvas.drawCircle(width / 4f + birdRadius/3, birdY - birdRadius/4, birdRadius/2.5f, eyePaint)
            canvas.drawCircle(width / 4f + birdRadius/3, birdY - birdRadius/4, birdRadius/5, pupilPaint)

            // 小鸟高光
            val highlightPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                alpha = 150
            }
            canvas.drawCircle(width / 4f + birdRadius/4, birdY - birdRadius/3, birdRadius/8, highlightPaint)

            // 小鸟嘴巴
            val beakPaint = Paint().apply {
                color = Color.parseColor("#FFEB3B")
                isAntiAlias = true
            }
            val beakPath = Path()
            beakPath.moveTo(width / 4f + birdRadius/1.5f, birdY)
            beakPath.lineTo(width / 4f + birdRadius, birdY - birdRadius/4)
            beakPath.lineTo(width / 4f + birdRadius, birdY + birdRadius/4)
            beakPath.close()
            canvas.drawPath(beakPath, beakPaint)

            // 绘制说明或游戏结束信息
            when (gameState) {
                GameState.WAITING -> {
                    val message = "点击开始游戏"
                    canvas.drawText(message, width / 2f, height / 2f, textPaint)
                }
                GameState.GAME_OVER -> {
                    val message = "游戏结束\n点击重新开始"
                    canvas.drawText(message, width / 2f, height / 2f, textPaint)
                }
                else -> {}
            }
        }

        // 绘制云朵（更蓬松的效果）
        private fun drawClouds(canvas: Canvas) {
            for (cloud in clouds) {
                // 绘制更蓬松的云朵
                canvas.drawCircle(cloud.x, cloud.y, cloud.size/2, cloudPaint)
                canvas.drawCircle(cloud.x + cloud.size/3, cloud.y - cloud.size/6, cloud.size/2.2f, cloudPaint)
                canvas.drawCircle(cloud.x + cloud.size/2, cloud.y, cloud.size/2.5f, cloudPaint)
                canvas.drawCircle(cloud.x + cloud.size/2.5f, cloud.y + cloud.size/8, cloud.size/2.8f, cloudPaint)
                canvas.drawCircle(cloud.x + cloud.size/1.5f, cloud.y - cloud.size/10, cloud.size/2.3f, cloudPaint)
                canvas.drawCircle(cloud.x + cloud.size, cloud.y, cloud.size/2.5f, cloudPaint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    when (gameState) {
                        GameState.WAITING -> {
                            startGame()
                        }
                        GameState.PLAYING -> {
                            birdVelocity = jumpStrength
                        }
                        GameState.GAME_OVER -> {
                            resetGame()
                        }
                    }
                }
            }
            return true
        }

        fun update() {
            if (gameState != GameState.PLAYING) return

            // 更新小鸟
            birdVelocity += gravity
            birdY += birdVelocity

            // 检查地面碰撞
            if (birdY > height - groundHeight) {
                birdY = height - groundHeight
                gameOver()
            }

            // 检查天花板碰撞
            if (birdY < 0) {
                birdY = 0f
                birdVelocity = 0f
            }

            // 更新管道
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPipeTime > pipeInterval) {
                addPipe()
                lastPipeTime = currentTime
            }

            // 移动管道并检查碰撞
            val birdRadius = min(width, height) / 18f
            val birdX = width / 4f

            for (pipe in pipes) {
                pipe.x -= pipeSpeed

                // 检查小鸟是否通过管道
                if (!pipe.passed && pipe.x + pipeWidth < birdX) {
                    pipe.passed = true
                }

                // 检查碰撞
                if (birdX + birdRadius > pipe.x &&
                    birdX - birdRadius < pipe.x + pipeWidth) {
                    if (birdY - birdRadius < pipe.topHeight ||
                        birdY + birdRadius > pipe.topHeight + pipeGap) {
                        gameOver()
                    }
                }
            }

            // 移除屏幕外的管道
            pipes.removeAll { it.x + pipeWidth < 0 }

            // 移动云朵
            for (cloud in clouds) {
                cloud.x -= 1f
                if (cloud.x + cloud.size < 0) {
                    cloud.x = width + cloud.size
                    cloud.y = Random.nextFloat() * (height / 3)
                }
            }
        }

        private fun addPipe() {
            val minHeight = 150f
            val maxHeight = height - groundHeight - pipeGap - minHeight
            val topHeight = Random.nextFloat() * (maxHeight - minHeight) + minHeight
            pipes.add(Pipe(width.toFloat(), topHeight))
        }

        private fun startGame() {
            gameState = GameState.PLAYING
        }

        private fun gameOver() {
            gameState = GameState.GAME_OVER
        }

        private fun resetGame() {
            birdY = height / 2f
            birdVelocity = 0f
            pipes.clear()
            lastPipeTime = 0L
            gameState = GameState.WAITING

            // 重新初始化云朵
            clouds.clear()
            for (i in 0..5) {
                clouds.add(
                    Cloud(
                    Random.nextFloat() * width,
                    Random.nextFloat() * (height / 3),
                    80f + Random.nextFloat() * 70f
                )
                )
            }
        }
    }
}
