#!/usr/bin/env python3
"""
Glyph Toy Eyes Animation Frame Generator - CSV Output
Creates CSV files for pupil movement: right → center → left → center
"""

import math
import csv

def create_eye_frame(left_pupil_offset_x=0, right_pupil_offset_x=0, left_pupil_offset_y=0, right_pupil_offset_y=0):
    """
    Create a 25x25 frame with eyes and pupils at specified offsets
    """
    # Initialize 25x25 matrix with zeros
    frame = [[0 for _ in range(25)] for _ in range(25)]
    
    # Eye parameters from GlyphEyesService.kt
    eye_center_left = (8, 12)
    eye_center_right = (17, 12)
    eye_radius_x = 4
    eye_radius_y = 6
    pupil_radius = 2
    
    # Calculate actual pupil positions
    left_pupil_center = (eye_center_left[0] + left_pupil_offset_x, eye_center_left[1] + left_pupil_offset_y)
    right_pupil_center = (eye_center_right[0] + right_pupil_offset_x, eye_center_right[1] + right_pupil_offset_y)
    
    # Helper function to check if point is inside ellipse
    def is_inside_ellipse(x, y, center_x, center_y, radius_x, radius_y):
        dx = x - center_x
        dy = y - center_y
        return (dx * dx) / (radius_x * radius_x) + (dy * dy) / (radius_y * radius_y) <= 1.0
    
    # Helper function to check if point is inside circle
    def is_inside_circle(x, y, center_x, center_y, radius):
        dx = x - center_x
        dy = y - center_y
        return dx * dx + dy * dy <= radius * radius
    
    # Fill the frame
    for y in range(25):
        for x in range(25):
            pixel_value = 0
            
            # Check if pixel is inside left eye (white part)
            if is_inside_ellipse(x, y, eye_center_left[0], eye_center_left[1], eye_radius_x, eye_radius_y):
                pixel_value = 2040  # White eye
                
                # Check if pixel is inside left pupil (black part)
                if is_inside_circle(x, y, left_pupil_center[0], left_pupil_center[1], pupil_radius):
                    pixel_value = 0  # Black pupil
            
            # Check if pixel is inside right eye (white part)
            elif is_inside_ellipse(x, y, eye_center_right[0], eye_center_right[1], eye_radius_x, eye_radius_y):
                pixel_value = 2040  # White eye
                
                # Check if pixel is inside right pupil (black part)
                if is_inside_circle(x, y, right_pupil_center[0], right_pupil_center[1], pupil_radius):
                    pixel_value = 0  # Black pupil
            
            # Check if pixel is in the connection area between eyes
            elif (eye_center_left[0] + eye_radius_x <= x <= eye_center_right[0] - eye_radius_x and 
                  abs(y - eye_center_left[1]) <= eye_radius_y // 2):
                pixel_value = 2040  # White connection
            
            frame[y][x] = pixel_value
    
    return frame

def frame_to_single_row(frame):
    """Convert 25x25 frame to single row (625 values)"""
    values = []
    for row in frame:
        for value in row:
            values.append(value)
    return values

def main():
    """Generate animation frames and save as CSV"""
    print("Generating Glyph Toy Eyes Animation Frames as CSV...")
    
    # Frame 1: Pupils looking right (2 pixels)
    frame1 = create_eye_frame(left_pupil_offset_x=2, right_pupil_offset_x=2)
    
    # Frame 2: Pupils center
    frame2 = create_eye_frame(left_pupil_offset_x=0, right_pupil_offset_x=0)
    
    # Frame 3: Pupils looking left (2 pixels)
    frame3 = create_eye_frame(left_pupil_offset_x=-2, right_pupil_offset_x=-2)
    
    # Frame 4: Pupils center again
    frame4 = create_eye_frame(left_pupil_offset_x=0, right_pupil_offset_x=0)
    
    frames = [frame1, frame2, frame3, frame4]
    frame_names = ["Frame1_Looking_Right", "Frame2_Center", "Frame3_Looking_Left", "Frame4_Center"]
    
    # Save each frame as individual CSV files
    for i, (frame, name) in enumerate(zip(frames, frame_names)):
        filename = f"glyph_eyes_animation_{name.lower()}.csv"
        with open(filename, 'w', newline='') as csvfile:
            writer = csv.writer(csvfile)
            # Write header
            writer.writerow([f"Glyph Eyes Animation - {name}"])
            writer.writerow([f"25x25 matrix with 625 values"])
            writer.writerow([f"0=OFF(Black), 2040=ON(White)"])
            writer.writerow([])  # Empty row
            
            # Write single row with all 625 values
            single_row = frame_to_single_row(frame)
            writer.writerow(single_row)
        
        print(f"✅ Saved: {filename}")
    
    # Save all frames in one CSV file
    all_frames_filename = "glyph_eyes_animation_all_frames.csv"
    with open(all_frames_filename, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        # Write header
        writer.writerow(["Glyph Eyes Animation - All Frames"])
        writer.writerow(["Each row represents one frame (625 values per frame)"])
        writer.writerow(["Frame1: Looking Right, Frame2: Center, Frame3: Looking Left, Frame4: Center"])
        writer.writerow(["0=OFF(Black), 2040=ON(White)"])
        writer.writerow([])  # Empty row
        
        # Write frame labels and data
        for i, (frame, name) in enumerate(zip(frames, frame_names)):
            writer.writerow([f"=== {name} ==="])
            single_row = frame_to_single_row(frame)
            writer.writerow(single_row)
            writer.writerow([])  # Empty row between frames
    
    print(f"✅ Saved: {all_frames_filename}")
    
    # Also create a simple format CSV (just the data)
    simple_filename = "glyph_eyes_animation_simple.csv"
    with open(simple_filename, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        for frame in frames:
            single_row = frame_to_single_row(frame)
            writer.writerow(single_row)
    
    print(f"✅ Saved: {simple_filename}")
    print("\n📁 Generated CSV files:")
    print("   - Individual frame files: glyph_eyes_animation_frame*.csv")
    print("   - All frames with labels: glyph_eyes_animation_all_frames.csv") 
    print("   - Simple format (data only): glyph_eyes_animation_simple.csv")

if __name__ == "__main__":
    main()