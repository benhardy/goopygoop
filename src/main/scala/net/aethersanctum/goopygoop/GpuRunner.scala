package net.aethersanctum.goopygoop

import org.jocl.CL._
import org.jocl._

class GpuContext {
  // The platform, device type and device number
  // that will be used
  val platformIndex = 0
  val deviceType: Long = CL_DEVICE_TYPE_ALL
  val deviceIndex = 0

  // Enable exceptions and subsequently omit error checks in this sample
  CL.setExceptionsEnabled(true)

  // Obtain the number of platforms
  val numPlatformsArray = new Array[Int](1)
  clGetPlatformIDs(0, null, numPlatformsArray)
  val numPlatforms = numPlatformsArray(0)

  // Obtain a platform ID
  val platforms = new Array[cl_platform_id](numPlatforms)
  clGetPlatformIDs(platforms.length, platforms, null)
  val platform = platforms(platformIndex)

  // Initialize the context properties
  val contextProperties = new cl_context_properties()
  contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform)

  // Obtain the number of devices for the platform
  val numDevicesArray = new Array[Int](1)
  clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray)
  val numDevices = numDevicesArray(0)

  // Obtain a device ID
  val devices = new Array[cl_device_id](numDevices)
  clGetDeviceIDs(platform, deviceType, numDevices, devices, null)
  val device = devices(deviceIndex)

  // Create a context for the selected device
  val context = clCreateContext(
    contextProperties, 1, Array[cl_device_id](device),
    null, null, null)

}

class GpuRunner(rendering: Rendering) {
  val kernelMainLines = List(
    "#define color float3",
    "",
    "__kernel void kernelMain(__global float *results) {",
    "    int taskId = get_global_id(0);",
    "    color paint;",
    "    paint.x = (float)(taskId &0xff);",
    "    paint.y = (float)((taskId>>2) &0xff);",
    "    paint.z = (float)((taskId>>4) &0xff);",
    "    int outputOffset = 3 * taskId;",
    "    results[outputOffset+0] = paint.x;",
    "    results[outputOffset+1] = paint.y;",
    "    results[outputOffset+2] = paint.z;",
    "}"
  )

  val colorVectorsPerRow = new Array[Float](3 * rendering.screenWidth)

  def run = {
    val contextSetup = new GpuContext
    val context = contextSetup.context

    // Create a command-queue for the selected device
    val commandQueue = clCreateCommandQueue(context, contextSetup.device, 0, null)

    // Allocate the memory objects for the input- and output data
    val memObjects = new Array[cl_mem](3)
    val singleColorSize = Sizeof.cl_float * 3
    val colorArraySize = singleColorSize * rendering.screenWidth

    val resultPointer = Pointer.to(colorVectorsPerRow)
    val resultBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, colorArraySize, resultPointer, null)

    val kernelSource = Array[String](kernelMainLines.mkString("\n"))
    // Create the program from the source code
    val program = clCreateProgramWithSource(context, 1, kernelSource, null, null)

    // Build the program
    clBuildProgram(program, 0, null, null, null, null)

    // Create the kernel
    val kernel = clCreateKernel(program, "kernelMain", null)

    for (row <- 0 until rendering.screenHeight) {
      printf("\rrendering row %d/%d", row, rendering.screenHeight)

      // Set the arguments for the kernel
      clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(resultBuffer))

      // Set the work-item dimensions
      val global_work_size = Array[Long](rendering.screenWidth)
      val local_work_size = Array[Long](1)

      // Execute the kernel
      clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null)

      // Read the output data
      clEnqueueReadBuffer(commandQueue, resultBuffer, CL_TRUE, 0, colorArraySize, resultPointer, 0, null, null)
      paintResults(row)
    }
    println("\nCleaning up openCL stuff")
    // Release kernel, program, and memory objects
    clReleaseMemObject(resultBuffer)
    clReleaseKernel(kernel)
    clReleaseProgram(program)
    clReleaseCommandQueue(commandQueue)
    clReleaseContext(context)
    println("\nDone")
  }

  def paintResults(row:Int): Unit = {
    for (x <- 0 until rendering.screenWidth) {
      val offset = 3 * x
      val red = colorVectorsPerRow(offset + 0)
      val green = colorVectorsPerRow(offset + 1)
      val blue = colorVectorsPerRow(offset + 2)
      rendering.setPixel(x, row, red, green, blue)
    }
  }
}

object GpuRunner {
  def main(args:Array[String]):Unit = {
    val rendering = new Rendering(640, 480)
    val runner = new GpuRunner(rendering)
    runner.run
    rendering.save("GpuRunner.png")
  }
}
